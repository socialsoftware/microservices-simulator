package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateExecutionRequestDto;

public class CreateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto createdExecutionDto;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateExecutionFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService, CreateExecutionRequestDto createRequest) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateExecutionRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createExecutionStep = new SagaSyncStep("createExecutionStep", () -> {
            ExecutionDto createdExecutionDto = executionService.createExecution(createRequest, unitOfWork);
            setCreatedExecutionDto(createdExecutionDto);
        });

        workflow.addStep(createExecutionStep);

    }

    public ExecutionDto getCreatedExecutionDto() {
        return createdExecutionDto;
    }

    public void setCreatedExecutionDto(ExecutionDto createdExecutionDto) {
        this.createdExecutionDto = createdExecutionDto;
    }
}
