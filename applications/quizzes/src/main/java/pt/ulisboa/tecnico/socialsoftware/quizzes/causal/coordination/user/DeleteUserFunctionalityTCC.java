package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalUser;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;

public class DeleteUserFunctionalityTCC extends WorkflowFunctionality {
    private CausalUser user;
    private final UserService userService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public DeleteUserFunctionalityTCC(UserService userService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            userService.deleteUser(userAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    

    public CausalUser getUser() {
        return user;
    }

    public void setUser(CausalUser user) {
        this.user = user;
    }
}