package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;

public class FindQuestionByAggregateIdFunctionalityTCC extends WorkflowFunctionality {
    private QuestionDto questionDto;
    private final QuestionService questionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public FindQuestionByAggregateIdFunctionalityTCC(QuestionService questionService, CausalUnitOfWorkService unitOfWorkService,  
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
    

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }
}