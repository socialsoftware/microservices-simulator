package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;

public class RemoveQuestionFunctionalityTCC extends WorkflowFunctionality {
    private CausalQuestion question;
    private final QuestionService questionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public RemoveQuestionFunctionalityTCC(QuestionService questionService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer questionAggregateId, CausalUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer questionAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            questionService.removeQuestion(questionAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public CausalQuestion getQuestion() {
        return question;
    }

    public void setQuestion(CausalQuestion question) {
        this.question = question;
    }
}