package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.CreateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto quizDto;
    private ExecutionDto executionDto;
    private final List<QuizQuestionDto> questions = new ArrayList<>();

    public CreateQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, QuizDto quizDto,
                                        List<Integer> questionAggregateIds, SagaUnitOfWork unitOfWork,
                                        CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, quizDto, questionAggregateIds, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, QuizDto quizDto,
                              List<Integer> questionAggregateIds, SagaUnitOfWork unitOfWork,
                              CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P4a prerequisite: seeds the QuizExecution snapshot with the execution's id and version. The
        // fetch itself is the check for COURSE_EXECUTION_EXISTS at create time — it throws when the
        // execution does not exist, so no explicit guard is written in QuizService; the P2
        // subscription covers deletion after the fact.
        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand cmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), quizDto.getExecutionAggregateId());
            this.executionDto = (ExecutionDto) commandGateway.send(cmd);
        });

        // P4a prerequisite: seeds each QuizQuestion snapshot with the question's cached version,
        // title and content.
        SagaStep getQuestionsStep = new SagaStep("getQuestionsStep", () -> {
            for (Integer questionAggregateId : questionAggregateIds) {
                GetQuestionByIdCommand cmd = new GetQuestionByIdCommand(
                        unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
                QuestionDto questionDto = (QuestionDto) commandGateway.send(cmd);
                this.questions.add(new QuizQuestionDto(questionDto.getAggregateId(), questionDto.getVersion(),
                        questionDto.getTitle(), questionDto.getContent()));
            }
        });

        SagaStep createQuizStep = new SagaStep("createQuizStep", () -> {
            CreateQuizCommand cmd = new CreateQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto, this.executionDto, this.questions);
            this.quizDto = (QuizDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getExecutionStep, getQuestionsStep)));

        this.workflow.addStep(getExecutionStep);
        this.workflow.addStep(getQuestionsStep);
        this.workflow.addStep(createQuizStep);
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }
}
