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

public class DeleteExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto deletedExecutionDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public DeleteExecutionFunctionalitySagas(ExecutionService executionService, SagaUnitOfWorkService unitOfWorkService, Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getExecutionStep = new SagaSyncStep("getExecutionStep", () -> {
            ExecutionDto deletedExecutionDto = executionService.getExecutionById(executionAggregateId, unitOfWork);
            setDeletedExecutionDto(deletedExecutionDto);
            unitOfWorkService.registerSagaState(executionAggregateId, ExecutionSagaState.READ_EXECUTION, unitOfWork);
        });

        getExecutionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(executionAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep deleteExecutionStep = new SagaSyncStep("deleteExecutionStep", () -> {
            executionService.deleteExecution(executionAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        workflow.addStep(getExecutionStep);
        workflow.addStep(deleteExecutionStep);
    }

    public ExecutionDto getDeletedExecutionDto() {
        return deletedExecutionDto;
    }

    public void setDeletedExecutionDto(ExecutionDto deletedExecutionDto) {
        this.deletedExecutionDto = deletedExecutionDto;
    }
}
