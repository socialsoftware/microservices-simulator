package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;

public class ConcludeQuizFunctionalityTCC extends WorkflowFunctionality {
    private CausalQuizAnswer quizAnswer;
    private final QuizAnswerService quizAnswerService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public ConcludeQuizFunctionalityTCC(QuizAnswerService quizAnswerService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer quizAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.quizAnswerService = quizAnswerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            quizAnswerService.concludeQuiz(quizAggregateId, userAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public CausalQuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(CausalQuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}