package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.ExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class ExecutionSagaCoordination extends WorkflowFunctionality {
private ExecutionDto executionDto;
private SagaExecutionDto execution;
private final ExecutionService executionService;
private final SagaUnitOfWorkService unitOfWorkService;

public ExecutionSagaCoordination(ExecutionService executionService, SagaUnitOfWorkService
unitOfWorkService,
ExecutionDto executionDto, SagaUnitOfWork unitOfWork) {
this.executionService = executionService;
this.unitOfWorkService = unitOfWorkService;
this.buildWorkflow(executionDto, unitOfWork);
}

public void buildWorkflow(ExecutionDto executionDto, SagaUnitOfWork unitOfWork) {
this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
// Saga coordination logic will be implemented here
}

// Getters and setters
public ExecutionDto getExecutionDto() {
return executionDto;
}

public void setExecutionDto(ExecutionDto executionDto) {
this.executionDto = executionDto;
}

public SagaExecutionDto getExecution() {
return execution;
}

public void setExecution(SagaExecutionDto execution) {
this.execution = execution;
}
}