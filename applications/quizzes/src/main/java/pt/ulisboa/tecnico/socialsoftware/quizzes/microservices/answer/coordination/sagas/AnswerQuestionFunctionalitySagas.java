package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.AnswerQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.GetQuizAnswerDtoByQuizIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.sagas.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.states.QuizAnswerSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class AnswerQuestionFunctionalitySagas extends WorkflowFunctionality {

    private QuestionDto questionDto;
    private QuizAnswerDto quizAnswer;
    private final QuizAnswerService quizAnswerService;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AnswerQuestionFunctionalitySagas(QuizAnswerService quizAnswerService, QuestionService questionService,
            SagaUnitOfWorkService unitOfWorkService, QuizAnswerFactory quizAnswerFactory,
            Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.quizAnswerService = quizAnswerService;
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, userAggregateId, userQuestionAnswerDto, quizAnswerFactory, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto,
            QuizAnswerFactory quizAnswerFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
            // QuestionDto questionDto = (QuestionDto)
            // questionService.getQuestionById(userQuestionAnswerDto.getQuestionAggregateId(),
            // unitOfWork);
            // unitOfWorkService.registerSagaState(userQuestionAnswerDto.getQuestionAggregateId(),
            // QuestionSagaState.READ_QUESTION, unitOfWork);
            GetQuestionByIdCommand getQuestionByIdCommand = new GetQuestionByIdCommand(unitOfWork,
                    ServiceMapping.QUESTION.getServiceName(), userQuestionAnswerDto.getQuestionAggregateId());
            getQuestionByIdCommand.setSemanticLock(QuestionSagaState.READ_QUESTION);
            QuestionDto questionDto = (QuestionDto) commandGateway.send(getQuestionByIdCommand);
            this.setQuestionDto(questionDto);
        });

        getQuestionStep.registerCompensation(() -> {
            // unitOfWorkService.registerSagaState(this.questionDto.getAggregateId(),
            // GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(),
                    this.questionDto.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep getQuizAnswerStep = new SagaSyncStep("getQuizAnswerStep", () -> {
            // QuizAnswerDto quizAnswer = (QuizAnswerDto)
            // quizAnswerService.getQuizAnswerDtoByQuizIdAndUserId(quizAggregateId,
            // userAggregateId, unitOfWork);
            // unitOfWorkService.registerSagaState(quizAggregateId,
            // QuizAnswerSagaState.READ_QUIZ_ANSWER, unitOfWork); TODO
            GetQuizAnswerDtoByQuizIdAndUserIdCommand getQuizAnswerDtoByQuizIdAndUserIdCommand = new GetQuizAnswerDtoByQuizIdAndUserIdCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAnswer.getQuizAggregateId(), quizAggregateId, userAggregateId);
            getQuizAnswerDtoByQuizIdAndUserIdCommand.setSemanticLock(QuizAnswerSagaState.READ_QUIZ_ANSWER);
            QuizAnswerDto quizAnswer = (QuizAnswerDto) commandGateway.send(getQuizAnswerDtoByQuizIdAndUserIdCommand);
            this.setQuizAnswer(quizAnswer);
        });

        getQuizAnswerStep.registerCompensation(() -> {
            // unitOfWorkService.registerSagaState(this.quizAnswer.getAggregateId(),
            // GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    this.quizAnswer.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep answerQuestionStep = new SagaSyncStep("answerQuestionStep", () -> {
            // quizAnswerService.answerQuestion(quizAggregateId, userAggregateId,
            // userQuestionAnswerDto, this.getQuestionDto(), unitOfWork);
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