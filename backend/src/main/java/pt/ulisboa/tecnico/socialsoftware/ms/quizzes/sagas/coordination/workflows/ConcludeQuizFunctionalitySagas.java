package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class ConcludeQuizFunctionalitySagas extends WorkflowFunctionality {
    public enum State implements SagaState {
        CONCLUDE_QUIZ_READ_QUIZ_ANSWER {
            @Override
            public String getStateName() {
                return "CONCLUDE_QUIZ_READ_QUIZ_ANSWER";
            }
        }
    }
    
    private SagaQuizAnswer quizAnswer;

    

    private final QuizAnswerService quizAnswerService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public ConcludeQuizFunctionalitySagas(QuizAnswerService quizAnswerService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer quizAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.quizAnswerService = quizAnswerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getQuizAnswerStep = new SyncStep("getQuizAnswerStep", () -> {
            SagaQuizAnswer quizAnswer = (SagaQuizAnswer) quizAnswerService.getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quizAnswer, State.CONCLUDE_QUIZ_READ_QUIZ_ANSWER, unitOfWork);
            this.setQuizAnswer(quizAnswer);
        });
    
        getQuizAnswerStep.registerCompensation(() -> {
            SagaQuizAnswer quizAnswer = this.getQuizAnswer();
            quizAnswer.setCompleted(false);
            unitOfWorkService.registerSagaState(quizAnswer, GenericSagaState.NOT_IN_SAGA, unitOfWork);
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

    

    public SagaQuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(SagaQuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}