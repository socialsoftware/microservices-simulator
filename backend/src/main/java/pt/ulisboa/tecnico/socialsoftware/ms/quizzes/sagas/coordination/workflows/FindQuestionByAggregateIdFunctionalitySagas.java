package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class FindQuestionByAggregateIdFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto questionDto;

    

    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public FindQuestionByAggregateIdFunctionalitySagas(QuestionService questionService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer aggregateId, SagaUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(aggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer aggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep findQuestionStep = new SagaSyncStep("findQuestionStep",() -> {
            QuestionDto questionDto = questionService.getQuestionById(aggregateId, unitOfWork);
            this.setQuestionDto(questionDto);
        });

        workflow.addStep(findQuestionStep);
    }

    @Override
    public void handleEvents() {

    }

    

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }
}