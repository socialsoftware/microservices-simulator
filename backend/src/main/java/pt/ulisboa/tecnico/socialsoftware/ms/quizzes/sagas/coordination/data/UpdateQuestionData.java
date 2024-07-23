package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionData extends WorkflowData {
    private Question oldQuestion;

    private SagaWorkflow workflow;

    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateQuestionData(QuestionService questionService, SagaUnitOfWorkService unitOfWorkService,  
                            QuestionFactory questionFactory, QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionFactory, questionDto, unitOfWork);
    }

    public void buildWorkflow(QuestionFactory questionFactory, QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getOldQuestionStep = new SyncStep(() -> {
            SagaQuestion oldQuestion = (SagaQuestion) unitOfWorkService.aggregateLoadAndRegisterRead(questionDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(oldQuestion, SagaState.UPDATE_QUESTION_READ_QUESTION, unitOfWork);
            this.setOldQuestion(oldQuestion);
        });
    
        getOldQuestionStep.registerCompensation(() -> {
            Question newQuestion = questionFactory.createQuestionFromExisting(this.getOldQuestion());
            unitOfWorkService.registerSagaState((SagaQuestion) newQuestion, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newQuestion);
        }, unitOfWork);
    
        SyncStep updateQuestionStep = new SyncStep(() -> {
            questionService.updateQuestion(questionDto, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldQuestionStep)));
    
        workflow.addStep(getOldQuestionStep);
        workflow.addStep(updateQuestionStep);
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

    public Question getOldQuestion() {
        return oldQuestion;
    }

    public void setOldQuestion(Question oldQuestion) {
        this.oldQuestion = oldQuestion;
    }
}