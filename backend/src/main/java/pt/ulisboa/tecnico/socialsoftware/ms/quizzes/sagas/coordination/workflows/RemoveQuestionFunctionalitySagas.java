package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveQuestionFunctionalitySagas extends WorkflowFunctionality {

    private SagaQuestionDto question;

    

    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public RemoveQuestionFunctionalitySagas(QuestionService questionService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
            SagaQuestionDto question = (SagaQuestionDto) questionService.getQuestionById(questionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(questionAggregateId, QuestionSagaState.READ_QUESTION, unitOfWork);
            this.setQuestion(question);
        });
    
        getQuestionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(questionAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep removeQuestionStep = new SagaSyncStep("removeQuestionStep", () -> {
            questionService.removeQuestion(questionAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuestionStep)));
    
        workflow.addStep(getQuestionStep);
        workflow.addStep(removeQuestionStep);
    }

    @Override
    public void handleEvents() {

    }

    public SagaQuestionDto getQuestion() {
        return question;
    }

    public void setQuestion(SagaQuestionDto question) {
        this.question = question;
    }
}