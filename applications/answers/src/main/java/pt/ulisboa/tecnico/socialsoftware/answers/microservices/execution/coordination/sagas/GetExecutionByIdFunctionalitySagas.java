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

public class GetExecutionByIdFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto executionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetExecutionByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer executionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            unitOfWorkService.verifySagaState(executionAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(ExecutionSagaState.UPDATE_EXECUTION, ExecutionSagaState.DELETE_EXECUTION)));
            unitOfWorkService.registerSagaState(executionAggregateId, ExecutionSagaState.READ_EXECUTION, unitOfWork);
            GetExecutionByIdCommand cmd = new GetExecutionByIdCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            ExecutionDto executionDto = (ExecutionDto) commandGateway.send(cmd);
            setExecutionDto(executionDto);
        });

        workflow.addStep(getExecutionStep);
    }
    public ExecutionDto getExecutionDto() {
        return executionDto;
    }

    public void setExecutionDto(ExecutionDto executionDto) {
        this.executionDto = executionDto;
    }
}
