package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer.AnswerQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer.GetQuizAnswerDtoByQuizIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.question.GetQuestionByIdCommand;
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
            SagaCommand sagaCommand = new SagaCommand(getQuestionByIdCommand);
            sagaCommand.setSemanticLock(QuestionSagaState.READ_QUESTION);
            QuestionDto questionDto = (QuestionDto) commandGateway.send(sagaCommand);
            this.setQuestionDto(questionDto);
        });

        getQuestionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(),
                    this.questionDto.getAggregateId());
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep getQuizAnswerStep = new SagaStep("getQuizAnswerStep", () -> {
            GetQuizAnswerDtoByQuizIdAndUserIdCommand getQuizAnswerDtoByQuizIdAndUserIdCommand = new GetQuizAnswerDtoByQuizIdAndUserIdCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAnswer.getQuizAggregateId(), quizAggregateId, userAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getQuizAnswerDtoByQuizIdAndUserIdCommand);
            sagaCommand.setSemanticLock(QuizAnswerSagaState.READ_QUIZ_ANSWER);
            QuizAnswerDto quizAnswer = (QuizAnswerDto) commandGateway.send(sagaCommand);
            this.setQuizAnswer(quizAnswer);
        });

        getQuizAnswerStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    this.quizAnswer.getAggregateId());
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep answerQuestionStep = new SagaStep("answerQuestionStep", () -> {
            AnswerQuestionCommand answerQuestion = new AnswerQuestionCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), quizAggregateId, userAggregateId, userQuestionAnswerDto,
                    this.getQuestionDto());
            SagaCommand sagaCommand = new SagaCommand(answerQuestion);
            commandGateway.send(sagaCommand);
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