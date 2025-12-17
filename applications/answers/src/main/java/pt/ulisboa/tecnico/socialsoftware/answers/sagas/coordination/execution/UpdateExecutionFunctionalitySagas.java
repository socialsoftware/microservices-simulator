package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.ExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class UpdateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto updatedExecutionDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateExecutionFunctionalitySagas(ExecutionService executionService, SagaUnitOfWorkService unitOfWorkService, Integer executionAggregateId, ExecutionDto executionDto, SagaUnitOfWork unitOfWork) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, executionDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, ExecutionDto executionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getExecutionStep = new SagaSyncStep("getExecutionStep", () -> {
            unitOfWorkService.registerSagaState(executionAggregateId, ExecutionSagaState.READ_EXECUTION, unitOfWork);
        });

        getExecutionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(executionAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep updateExecutionStep = new SagaSyncStep("updateExecutionStep", () -> {
            ExecutionDto updatedExecutionDto = executionService.updateExecution(executionAggregateId, executionDto, unitOfWork);
            setUpdatedExecutionDto(updatedExecutionDto);
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        workflow.addStep(getExecutionStep);
        workflow.addStep(updateExecutionStep);
    }

    public ExecutionDto getUpdatedExecutionDto() {
        return updatedExecutionDto;
    }

    public void setUpdatedExecutionDto(ExecutionDto updatedExecutionDto) {
        this.updatedExecutionDto = updatedExecutionDto;
    }
}
