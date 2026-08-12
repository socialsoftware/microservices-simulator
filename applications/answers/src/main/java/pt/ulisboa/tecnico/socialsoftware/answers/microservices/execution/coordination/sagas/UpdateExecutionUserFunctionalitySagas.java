package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.execution.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateExecutionUserFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionUserDto updatedUserDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateExecutionUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer executionId, Integer userAggregateId, ExecutionUserDto userDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionId, userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionId, Integer userAggregateId, ExecutionUserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateUserStep = new SagaStep("updateUserStep", () -> {
            UpdateExecutionUserCommand cmd = new UpdateExecutionUserCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionId, userAggregateId, userDto);
            ExecutionUserDto updatedUserDto = (ExecutionUserDto) commandGateway.send(cmd);
            setUpdatedUserDto(updatedUserDto);
        });

        workflow.addStep(updateUserStep);
    }
    public ExecutionUserDto getUpdatedUserDto() {
        return updatedUserDto;
    }

    public void setUpdatedUserDto(ExecutionUserDto updatedUserDto) {
        this.updatedUserDto = updatedUserDto;
    }
}
