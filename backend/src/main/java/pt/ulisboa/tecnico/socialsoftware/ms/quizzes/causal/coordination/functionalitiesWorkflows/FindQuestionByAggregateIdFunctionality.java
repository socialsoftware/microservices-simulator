package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalitiesWorkflows;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;

public class FindQuestionByAggregateIdFunctionality extends WorkflowFunctionality {
    private QuestionDto questionDto;

    private CausalWorkflow workflow;

    private final QuestionService questionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public FindQuestionByAggregateIdFunctionality(QuestionService questionService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer aggregateId, CausalUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(aggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer aggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.questionDto = questionService.getQuestionById(aggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(CausalUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, CausalUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, CausalUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(CausalUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }
}