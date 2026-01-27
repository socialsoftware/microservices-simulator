package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetExecutionByIdFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto executionDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetExecutionByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, Integer executionAggregateId) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getExecutionStep = new SagaSyncStep("getExecutionStep", () -> {
            ExecutionDto executionDto = executionService.getExecutionById(executionAggregateId);
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
