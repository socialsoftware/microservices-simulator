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

public class AddExecutionUserFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionUserDto addedUserDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddExecutionUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer executionId, Integer userAggregateId, ExecutionUserDto userDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionId, userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionId, Integer userAggregateId, ExecutionUserDto userDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addUserStep = new SagaStep("addUserStep", () -> {
            AddExecutionUserCommand cmd = new AddExecutionUserCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionId, userAggregateId, userDto);
            ExecutionUserDto addedUserDto = (ExecutionUserDto) commandGateway.send(cmd);
            setAddedUserDto(addedUserDto);
        });

        workflow.addStep(addUserStep);
    }
    public ExecutionUserDto getAddedUserDto() {
        return addedUserDto;
    }

    public void setAddedUserDto(ExecutionUserDto addedUserDto) {
        this.addedUserDto = addedUserDto;
    }
}
