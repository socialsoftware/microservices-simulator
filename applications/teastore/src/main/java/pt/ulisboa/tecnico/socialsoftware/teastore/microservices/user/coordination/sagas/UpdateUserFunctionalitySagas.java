package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.user.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateUserFunctionalitySagas extends WorkflowFunctionality {
    private UserDto updatedUserDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, UserDto userDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(userDto, unitOfWork);
    }

    public void buildWorkflow(UserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateUserStep = new SagaStep("updateUserStep", () -> {
            UpdateUserCommand cmd = new UpdateUserCommand(unitOfWork, ServiceMapping.USER.getServiceName(), userDto);
            UserDto updatedUserDto = (UserDto) commandGateway.send(cmd);
            setUpdatedUserDto(updatedUserDto);
        });

        workflow.addStep(updateUserStep);
    }
    public UserDto getUpdatedUserDto() {
        return updatedUserDto;
    }

    public void setUpdatedUserDto(UserDto updatedUserDto) {
        this.updatedUserDto = updatedUserDto;
    }
}
