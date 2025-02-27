package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.question;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private SagaQuestionDto question;
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

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
            SagaQuestionDto question = (SagaQuestionDto) questionService.getQuestionById(questionDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(question.getAggregateId(), QuestionSagaState.READ_QUESTION, unitOfWork);
            this.setQuestion(question);
        });
    
        getQuestionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(question.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep updateQuestionStep = new SagaSyncStep("updateQuestionStep", () -> {
            questionService.updateQuestion(questionDto, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuestionStep)));
    
        workflow.addStep(getQuestionStep);
        workflow.addStep(updateQuestionStep);
    }
    

    public SagaQuestionDto getQuestion() {
        return question;
    }

    public void setQuestion(SagaQuestionDto question) {
        this.question = question;
    }
}