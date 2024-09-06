package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class StartQuizFunctionality extends WorkflowFunctionality {
    private QuizDto quizDto;

    private SagaWorkflow workflow;

    private final QuizAnswerService quizAnswerService;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public StartQuizFunctionality(QuizAnswerService quizAnswerService, QuizService quizService, SagaUnitOfWorkService unitOfWorkService,  
                        Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.quizAnswerService = quizAnswerService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getQuizStep = new SyncStep("getQuizStep", () -> {
            QuizDto quizDto = quizService.getQuizById(quizAggregateId, unitOfWork);
            SagaQuiz quiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quiz, SagaState.START_QUIZ_READ_QUIZ, unitOfWork);
            this.setQuizDto(quizDto);
        });
    
        getQuizStep.registerCompensation(() -> {
            SagaQuiz quiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quiz, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SyncStep startQuizStep = new SyncStep("startQuizStep", () -> {
            quizAnswerService.startQuiz(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuizStep)));
    
        startQuizStep.registerCompensation(() -> {
            QuizAnswer quizAnswer = quizAnswerService.getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
            unitOfWork.registerChanged(quizAnswer);
        }, unitOfWork);
    
        workflow.addStep(getQuizStep);
        workflow.addStep(startQuizStep);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }
}