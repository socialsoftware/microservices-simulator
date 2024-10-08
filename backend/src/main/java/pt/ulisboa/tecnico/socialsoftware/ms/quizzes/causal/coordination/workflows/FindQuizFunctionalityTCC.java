package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.workflows;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;

public class FindQuizFunctionalityTCC extends WorkflowFunctionality {
    private QuizDto quizDto;
    private final QuizService quizService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public FindQuizFunctionalityTCC(QuizService quizService, CausalUnitOfWorkService unitOfWorkService,  
                        Integer quizAggregateId, CausalUnitOfWork unitOfWork) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.quizDto = quizService.getQuizById(quizAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }
}