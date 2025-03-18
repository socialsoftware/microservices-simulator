package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaQuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class StartQuizFunctionalitySagas extends WorkflowFunctionality {
    
    private SagaQuizDto quizDto;
    private final QuizAnswerService quizAnswerService;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public StartQuizFunctionalitySagas(QuizAnswerService quizAnswerService, QuizService quizService, SagaUnitOfWorkService unitOfWorkService,  
                        Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.quizAnswerService = quizAnswerService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuizStep = new SagaSyncStep("getQuizStep", () -> {
            SagaQuizDto quizDto = (SagaQuizDto) quizService.getQuizById(quizAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quizAggregateId, QuizSagaState.READ_QUIZ, unitOfWork);
            this.setQuizDto(quizDto);
        });
    
        getQuizStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(quizAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep startQuizStep = new SagaSyncStep("startQuizStep", () -> {
            quizAnswerService.startQuiz(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuizStep)));
    
        workflow.addStep(getQuizStep);
        workflow.addStep(startQuizStep);
    }
    

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(SagaQuizDto quizDto) {
        this.quizDto = quizDto;
    }
}