package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.AnswerQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.GetQuizAnswerDtoByQuizIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.states.QuizAnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.sagas.states.QuestionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class AnswerQuestionFunctionalitySagas extends WorkflowFunctionality {

    private QuestionDto questionDto;
    private QuizAnswerDto quizAnswer;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AnswerQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, QuizAnswerFactory quizAnswerFactory,
                                            Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, userAggregateId, userQuestionAnswerDto, quizAnswerFactory, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto,
            QuizAnswerFactory quizAnswerFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuestionStep = new SagaStep("getQuestionStep", () -> {
            GetQuestionByIdCommand getQuestionByIdCommand = new GetQuestionByIdCommand(unitOfWork,
                    ServiceMapping.QUESTION.getServiceName(), userQuestionAnswerDto.getQuestionAggregateId());
            getQuestionByIdCommand.setSemanticLock(QuestionSagaState.READ_QUESTION);
            QuestionDto questionDto = (QuestionDto) commandGateway.send(getQuestionByIdCommand);
            this.setQuestionDto(questionDto);
        });

        getQuestionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(),
                    this.questionDto.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep getQuizAnswerStep = new SagaStep("getQuizAnswerStep", () -> {
            GetQuizAnswerDtoByQuizIdAndUserIdCommand getQuizAnswerDtoByQuizIdAndUserIdCommand = new GetQuizAnswerDtoByQuizIdAndUserIdCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAnswer.getQuizAggregateId(), quizAggregateId, userAggregateId);
            getQuizAnswerDtoByQuizIdAndUserIdCommand.setSemanticLock(QuizAnswerSagaState.READ_QUIZ_ANSWER);
            QuizAnswerDto quizAnswer = (QuizAnswerDto) commandGateway.send(getQuizAnswerDtoByQuizIdAndUserIdCommand);
            this.setQuizAnswer(quizAnswer);
        });

        getQuizAnswerStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    this.quizAnswer.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep answerQuestionStep = new SagaStep("answerQuestionStep", () -> {
            AnswerQuestionCommand answerQuestion = new AnswerQuestionCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), quizAggregateId, userAggregateId, userQuestionAnswerDto,
                    this.getQuestionDto());
            commandGateway.send(answerQuestion);
        }, new ArrayList<>(Arrays.asList(getQuestionStep, getQuizAnswerStep)));

        workflow.addStep(getQuestionStep);
        workflow.addStep(getQuizAnswerStep);
        workflow.addStep(answerQuestionStep);
    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }

    public QuizAnswerDto getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(QuizAnswerDto quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}