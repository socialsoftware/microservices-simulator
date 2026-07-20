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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.UpdateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.states.QuizSagaState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto quizDto;
    private Set<QuizQuestion> quizQuestions;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         Integer quizAggregateId, LocalDateTime availableDate,
                                         LocalDateTime conclusionDate, LocalDateTime resultsDate,
                                         List<Integer> questionIds,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, availableDate, conclusionDate, resultsDate, questionIds, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, LocalDateTime availableDate,
                               LocalDateTime conclusionDate, LocalDateTime resultsDate,
                               List<Integer> questionIds, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizStep = new SagaStep("getQuizStep", () -> {
            GetQuizByIdCommand getCmd = new GetQuizByIdCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCmd);
            sagaCommand.setSemanticLock(QuizSagaState.IN_UPDATE_QUIZ);
            this.quizDto = (QuizDto) commandGateway.send(sagaCommand);
        });

        SagaStep getQuestionsStep = new SagaStep("getQuestionsStep", () -> {
            this.quizQuestions = new HashSet<>();
            for (Integer questionId : questionIds) {
                GetQuestionByIdCommand getQuestionCmd = new GetQuestionByIdCommand(
                        unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId);
                QuestionDto questionDto = (QuestionDto) commandGateway.send(getQuestionCmd);
                this.quizQuestions.add(new QuizQuestion(questionDto));
            }
        }, new ArrayList<>(Arrays.asList(getQuizStep)));

        SagaStep updateQuizStep = new SagaStep("updateQuizStep", () -> {
            UpdateQuizCommand updateCmd = new UpdateQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    quizAggregateId, availableDate, conclusionDate, resultsDate, this.quizQuestions);
            commandGateway.send(updateCmd);
        }, new ArrayList<>(Arrays.asList(getQuestionsStep)));

        workflow.addStep(getQuizStep);
        workflow.addStep(getQuestionsStep);
        workflow.addStep(updateQuizStep);
    }

    public QuizDto getQuizDto() { return quizDto; }
}
