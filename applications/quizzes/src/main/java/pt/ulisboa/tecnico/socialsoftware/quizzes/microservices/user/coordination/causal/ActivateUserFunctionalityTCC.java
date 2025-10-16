package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.causal.CausalUser;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.ActivateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

@SuppressWarnings("unused")
public class ActivateUserFunctionalityTCC extends WorkflowFunctionality {
    private CausalUser user;
    private final UserService userService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public ActivateUserFunctionalityTCC(UserService userService, CausalUnitOfWorkService unitOfWorkService,
            Integer userAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.userService = userService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // userService.activateUser(userAggregateId, unitOfWork);
            ActivateUserCommand ActivateUserCommand = new ActivateUserCommand(unitOfWork,
                    ServiceMapping.USER.getServiceName(), userAggregateId);
            commandGateway.send(ActivateUserCommand);
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