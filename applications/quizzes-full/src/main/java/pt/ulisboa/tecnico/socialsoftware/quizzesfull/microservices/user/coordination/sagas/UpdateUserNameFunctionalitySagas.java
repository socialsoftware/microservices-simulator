package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.UpdateUserNameCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.states.UserSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateUserNameFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateUserNameFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            Integer userAggregateId, String newName,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userAggregateId, newName, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, String newName, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand getCmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCmd);
            sagaCommand.setSemanticLock(UserSagaState.READ_USER);
            this.userDto = (UserDto) commandGateway.send(sagaCommand);
        });

        SagaStep updateUserNameStep = new SagaStep("updateUserNameStep", () -> {
            UpdateUserNameCommand cmd = new UpdateUserNameCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId, newName);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        workflow.addStep(getUserStep);
        workflow.addStep(updateUserNameStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
