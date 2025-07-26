package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaQuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.QuizAnswerSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class ConcludeQuizFunctionalitySagas extends WorkflowFunctionality {
    
    private SagaQuizAnswerDto quizAnswer;
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

        SagaSyncStep getQuizAnswerStep = new SagaSyncStep("getQuizAnswerStep", () -> {
            SagaQuizAnswerDto quizAnswer = (SagaQuizAnswerDto) quizAnswerService.getQuizAnswerDtoByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quizAnswer.getAggregateId(), QuizAnswerSagaState.READ_QUIZ_ANSWER, unitOfWork);
            this.setQuizAnswer(quizAnswer);
        });
    
        getQuizAnswerStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(this.quizAnswer.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep concludeQuizStep = new SagaSyncStep("concludeQuizStep", () -> {
            quizAnswerService.concludeQuiz(quizAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuizAnswerStep)));
    
        workflow.addStep(getQuizAnswerStep);
        workflow.addStep(concludeQuizStep);
    }
    

    public SagaQuizAnswerDto getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(SagaQuizAnswerDto quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}