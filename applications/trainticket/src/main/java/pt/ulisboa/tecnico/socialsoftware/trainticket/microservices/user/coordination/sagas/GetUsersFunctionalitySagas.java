package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.user.GetUsersCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto;

import java.util.List;

public class GetUsersFunctionalitySagas extends WorkflowFunctionality {
    private List<UserDto> users;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetUsersFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                      SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(unitOfWork);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUsersStep = new SagaStep("getUsersStep", () -> {
            GetUsersCommand cmd = new GetUsersCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName());
            this.users = (List<UserDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getUsersStep);
    }

    public List<UserDto> getUsers() {
        return users;
    }
}
