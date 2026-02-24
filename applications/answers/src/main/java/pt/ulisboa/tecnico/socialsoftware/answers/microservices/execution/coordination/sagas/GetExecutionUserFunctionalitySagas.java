package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.execution.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetExecutionUserFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionUserDto userDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetExecutionUserFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer executionId, Integer userAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetExecutionUserCommand cmd = new GetExecutionUserCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionId, userAggregateId);
            ExecutionUserDto userDto = (ExecutionUserDto) commandGateway.send(cmd);
            setUserDto(userDto);
        });

        workflow.addStep(getUserStep);
    }
    public ExecutionUserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(ExecutionUserDto userDto) {
        this.userDto = userDto;
    }
}
