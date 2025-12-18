package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.ExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.UserSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateAnswerFunctionalitySagas extends WorkflowFunctionality {
    private AnswerDto createdAnswerDto;
    private final AnswerService answerService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private SagaExecutionDto executionDto;
    private AnswerExecution execution;
    private final ExecutionService executionService;
    private SagaUserDto userDto;
    private AnswerUser user;
    private final UserService userService;
    private SagaQuizDto quizDto;
    private AnswerQuiz quiz;
    private final QuizService quizService;


    public CreateAnswerFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AnswerService answerService, ExecutionService executionService, UserService userService, QuizService quizService, Integer executionAggregateId, Integer userAggregateId, Integer quizAggregateId, AnswerDto answerDto) {
        this.answerService = answerService;
        this.unitOfWorkService = unitOfWorkService;
        this.executionService = executionService;
        this.userService = userService;
        this.quizService = quizService;
        this.buildWorkflow(executionAggregateId, userAggregateId, quizAggregateId, answerDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, Integer quizAggregateId, AnswerDto answerDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getExecutionStep = new SagaSyncStep("getExecutionStep", () -> {
            executionDto = (SagaExecutionDto) executionService.getExecutionById(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(executionDto.getAggregateId(), ExecutionSagaState.READ_EXECUTION, unitOfWork);
            AnswerExecution execution = new AnswerExecution(executionDto);
            setExecution(execution);
        });

        getExecutionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(executionDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            userDto = (SagaUserDto) userService.getUserById(userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(userDto.getAggregateId(), UserSagaState.READ_USER, unitOfWork);
            AnswerUser user = new AnswerUser(userDto);
            setUser(user);
        });

        getUserStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(userDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep getQuizStep = new SagaSyncStep("getQuizStep", () -> {
            quizDto = (SagaQuizDto) quizService.getQuizById(quizAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quizDto.getAggregateId(), QuizSagaState.READ_QUIZ, unitOfWork);
            AnswerQuiz quiz = new AnswerQuiz(quizDto);
            setQuiz(quiz);
        });

        getQuizStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(quizDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep createAnswerStep = new SagaSyncStep("createAnswerStep", () -> {
            AnswerDto createdAnswerDto = answerService.createAnswer(getExecution(), getUser(), getQuiz(), answerDto, unitOfWork);
            setCreatedAnswerDto(createdAnswerDto);
        }, new ArrayList<>(Arrays.asList(getExecutionStep, getUserStep, getQuizStep)));

        workflow.addStep(getExecutionStep);
        workflow.addStep(getUserStep);
        workflow.addStep(getQuizStep);
        workflow.addStep(createAnswerStep);

    }

    public AnswerExecution getExecution() {
        return execution;
    }

    public void setExecution(AnswerExecution execution) {
        this.execution = execution;
    }

    public AnswerUser getUser() {
        return user;
    }

    public void setUser(AnswerUser user) {
        this.user = user;
    }

    public AnswerQuiz getQuiz() {
        return quiz;
    }

    public void setQuiz(AnswerQuiz quiz) {
        this.quiz = quiz;
    }

    public AnswerDto getCreatedAnswerDto() {
        return createdAnswerDto;
    }

    public void setCreatedAnswerDto(AnswerDto createdAnswerDto) {
        this.createdAnswerDto = createdAnswerDto;
    }
}
