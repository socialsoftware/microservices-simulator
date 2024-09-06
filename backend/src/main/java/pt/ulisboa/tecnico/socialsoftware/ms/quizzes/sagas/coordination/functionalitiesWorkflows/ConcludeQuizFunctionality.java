package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class ConcludeQuizFunctionality extends WorkflowFunctionality {
    private SagaQuizAnswer quizAnswer;

    private SagaWorkflow workflow;

    private final QuizAnswerService quizAnswerService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public ConcludeQuizFunctionality(QuizAnswerService quizAnswerService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer quizAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.quizAnswerService = quizAnswerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getQuizAnswerStep = new SyncStep("getQuizAnswerStep", () -> {
            SagaQuizAnswer quizAnswer = (SagaQuizAnswer) quizAnswerService.getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quizAnswer, SagaState.CONCLUDE_QUIZ_READ_QUIZ_ANSWER, unitOfWork);
            this.setQuizAnswer(quizAnswer);
        });
    
        getQuizAnswerStep.registerCompensation(() -> {
            SagaQuizAnswer quizAnswer = this.getQuizAnswer();
            quizAnswer.setCompleted(false);
            unitOfWorkService.registerSagaState(quizAnswer, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(quizAnswer);
        }, unitOfWork);
    
        SyncStep concludeQuizStep = new SyncStep("concludeQuizStep", () -> {
            quizAnswerService.concludeQuiz(quizAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuizAnswerStep)));
    
        workflow.addStep(getQuizAnswerStep);
        workflow.addStep(concludeQuizStep);
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

    public SagaQuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(SagaQuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}