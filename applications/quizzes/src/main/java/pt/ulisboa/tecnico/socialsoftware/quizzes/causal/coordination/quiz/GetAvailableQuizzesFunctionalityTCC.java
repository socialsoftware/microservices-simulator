package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.quiz;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;

public class GetAvailableQuizzesFunctionalityTCC extends WorkflowFunctionality {
    private List<QuizDto> availableQuizzes;
    private final QuizService quizService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public GetAvailableQuizzesFunctionalityTCC(QuizService quizService, CausalUnitOfWorkService unitOfWorkService,  
                                    Integer userAggregateId, Integer courseExecutionAggregateId, CausalUnitOfWork unitOfWork) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, courseExecutionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, Integer courseExecutionAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.availableQuizzes = quizService.getAvailableQuizzes(courseExecutionAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public List<QuizDto> getAvailableQuizzes() {
        return availableQuizzes;
    }

    public void setAvailableQuizzes(List<QuizDto> availableQuizzes) {
        this.availableQuizzes = availableQuizzes;
    }
}
