package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto updatedExecutionDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateExecutionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, ExecutionDto executionDto) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionDto, unitOfWork);
    }

    public void buildWorkflow(ExecutionDto executionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateExecutionStep = new SagaSyncStep("updateExecutionStep", () -> {
            ExecutionDto updatedExecutionDto = executionService.updateExecution(executionDto, unitOfWork);
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
