package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.user.DeleteUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.causal.CausalUser;

public class DeleteUserFunctionalityTCC extends WorkflowFunctionality {
    private CausalUser user;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteUserFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                      Integer userAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            DeleteUserCommand DeleteUserCommand = new DeleteUserCommand(unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            commandGateway.send(DeleteUserCommand);
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