package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.user;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.CausalUser;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

public class ActivateUserFunctionalityTCC extends WorkflowFunctionality {
    private CausalUser user;
    private final UserService userService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public ActivateUserFunctionalityTCC(UserService userService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            userService.activateUser(userAggregateId, unitOfWork);
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