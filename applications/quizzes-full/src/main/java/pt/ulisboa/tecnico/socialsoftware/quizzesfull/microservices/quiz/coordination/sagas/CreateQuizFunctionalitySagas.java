package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.CreateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states.ExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateQuizFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto executionDto;
    private Set<QuizQuestion> quizQuestions;
    private QuizDto createdQuizDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         String title, LocalDateTime availableDate,
                                         LocalDateTime conclusionDate, LocalDateTime resultsDate,
                                         String quizType, Integer executionId, List<Integer> questionIds,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(title, availableDate, conclusionDate, resultsDate, quizType, executionId, questionIds, unitOfWork);
    }

    public void buildWorkflow(String title, LocalDateTime availableDate,
                               LocalDateTime conclusionDate, LocalDateTime resultsDate,
                               String quizType, Integer executionId, List<Integer> questionIds,
                               SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand getCmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionId);
            SagaCommand sagaCommand = new SagaCommand(getCmd);
            sagaCommand.setSemanticLock(ExecutionSagaState.READ_EXECUTION);
            this.executionDto = (ExecutionDto) commandGateway.send(sagaCommand);
        });

        getExecutionStep.registerCompensation(() -> {
            Command releaseCmd = new Command(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionId);
            SagaCommand release = new SagaCommand(releaseCmd);
            release.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(release);
        }, unitOfWork);

        SagaStep getQuestionsStep = new SagaStep("getQuestionsStep", () -> {
            this.quizQuestions = new HashSet<>();
            for (Integer questionId : questionIds) {
                GetQuestionByIdCommand getQuestionCmd = new GetQuestionByIdCommand(
                        unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId);
                QuestionDto questionDto = (QuestionDto) commandGateway.send(getQuestionCmd);
                this.quizQuestions.add(new QuizQuestion(questionDto));
            }
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        SagaStep createQuizStep = new SagaStep("createQuizStep", () -> {
            QuizExecution quizExecution = new QuizExecution(this.executionDto);
            CreateQuizCommand createCmd = new CreateQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    title, availableDate, conclusionDate, resultsDate,
                    QuizType.valueOf(quizType), quizExecution, this.quizQuestions);
            this.createdQuizDto = (QuizDto) commandGateway.send(createCmd);
        }, new ArrayList<>(Arrays.asList(getQuestionsStep)));

        workflow.addStep(getExecutionStep);
        workflow.addStep(getQuestionsStep);
        workflow.addStep(createQuizStep);
    }

    public QuizDto getCreatedQuizDto() { return createdQuizDto; }
}
