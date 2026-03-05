package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.execution.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto;

public class CreateExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto createdExecutionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateExecutionRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateExecutionRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createExecutionStep = new SagaStep("createExecutionStep", () -> {
            CreateExecutionCommand cmd = new CreateExecutionCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), createRequest);
            ExecutionDto createdExecutionDto = (ExecutionDto) commandGateway.send(cmd);
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
