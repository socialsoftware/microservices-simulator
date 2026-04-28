package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.execution.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.sagas.states.ExecutionSagaState;

public class UpdateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto updatedExecutionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, ExecutionDto executionDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionDto, unitOfWork);
    }

    public void buildWorkflow(ExecutionDto executionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateExecutionStep = new SagaStep("updateExecutionStep", () -> {
            unitOfWorkService.verifySagaState(executionDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(ExecutionSagaState.READ_EXECUTION, ExecutionSagaState.UPDATE_EXECUTION, ExecutionSagaState.DELETE_EXECUTION)));
            unitOfWorkService.registerSagaState(executionDto.getAggregateId(), ExecutionSagaState.UPDATE_EXECUTION, unitOfWork);
            UpdateExecutionCommand cmd = new UpdateExecutionCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionDto);
            ExecutionDto updatedExecutionDto = (ExecutionDto) commandGateway.send(cmd);
            setUpdatedExecutionDto(updatedExecutionDto);
        });

        workflow.addStep(updateExecutionStep);
    }
    public ExecutionDto getUpdatedExecutionDto() {
        return updatedExecutionDto;
    }

    public void setUpdatedExecutionDto(ExecutionDto updatedExecutionDto) {
        this.updatedExecutionDto = updatedExecutionDto;
    }
}
