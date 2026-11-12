package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.user.UpdateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.sagas.states.UserSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        Integer userAggregateId, UserDto updatedUserDto,
                                        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(userAggregateId, updatedUserDto, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, UserDto updatedUserDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand readCmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(UserSagaState.IN_UPDATE_USER);
            this.userDto = (UserDto) commandGateway.send(sagaCommand);
        });

        SagaStep updateUserStep = new SagaStep("updateUserStep", () -> {
            UpdateUserCommand cmd = new UpdateUserCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(),
                    userAggregateId, updatedUserDto);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        this.workflow.addStep(getUserStep);
        this.workflow.addStep(updateUserStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
