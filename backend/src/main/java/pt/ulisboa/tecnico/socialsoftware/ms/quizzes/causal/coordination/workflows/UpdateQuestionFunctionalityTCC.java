package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;

public class UpdateQuestionFunctionalityTCC extends WorkflowFunctionality {
    private Question oldQuestion;
    private final QuestionService questionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public UpdateQuestionFunctionalityTCC(QuestionService questionService, CausalUnitOfWorkService unitOfWorkService,  
                            QuestionFactory questionFactory, QuestionDto questionDto, CausalUnitOfWork unitOfWork) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionFactory, questionDto, unitOfWork);
    }

    public void buildWorkflow(QuestionFactory questionFactory, QuestionDto questionDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            questionService.updateQuestion(questionDto, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public Question getOldQuestion() {
        return oldQuestion;
    }

    public void setOldQuestion(Question oldQuestion) {
        this.oldQuestion = oldQuestion;
    }
}