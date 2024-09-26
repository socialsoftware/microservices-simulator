package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private Question oldQuestion;

    

    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateQuestionFunctionalitySagas(QuestionService questionService, SagaUnitOfWorkService unitOfWorkService,  
                            QuestionFactory questionFactory, QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionFactory, questionDto, unitOfWork);
    }

    public void buildWorkflow(QuestionFactory questionFactory, QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getOldQuestionStep = new SagaSyncStep("getOldQuestionStep", () -> {
            SagaQuestion oldQuestion = (SagaQuestion) unitOfWorkService.aggregateLoadAndRegisterRead(questionDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(oldQuestion, QuestionSagaState.UPDATE_QUESTION_READ_QUESTION, unitOfWork);
            this.setOldQuestion(oldQuestion);
        });
    
        getOldQuestionStep.registerCompensation(() -> {
            Question newQuestion = questionFactory.createQuestionFromExisting(this.getOldQuestion());
            unitOfWorkService.registerSagaState((SagaQuestion) newQuestion, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newQuestion);
        }, unitOfWork);
    
        SagaSyncStep updateQuestionStep = new SagaSyncStep("updateQuestionStep", () -> {
            questionService.updateQuestion(questionDto, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldQuestionStep)));
    
        workflow.addStep(getOldQuestionStep);
        workflow.addStep(updateQuestionStep);
    }

    @Override
    public void handleEvents() {

    }

    

    public Question getOldQuestion() {
        return oldQuestion;
    }

    public void setOldQuestion(Question oldQuestion) {
        this.oldQuestion = oldQuestion;
    }
}