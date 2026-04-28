package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.command.author.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.sagas.states.AuthorSagaState;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.webapi.requestDtos.CreateAuthorRequestDto;

public class CreateAuthorFunctionalitySagas extends WorkflowFunctionality {
    private AuthorDto createdAuthorDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateAuthorFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateAuthorRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateAuthorRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createAuthorStep = new SagaStep("createAuthorStep", () -> {
            CreateAuthorCommand cmd = new CreateAuthorCommand(unitOfWork, ServiceMapping.AUTHOR.getServiceName(), createRequest);
            AuthorDto createdAuthorDto = (AuthorDto) commandGateway.send(cmd);
            setCreatedAuthorDto(createdAuthorDto);
        });

        workflow.addStep(createAuthorStep);
    }
    public AuthorDto getCreatedAuthorDto() {
        return createdAuthorDto;
    }

    public void setCreatedAuthorDto(AuthorDto createdAuthorDto) {
        this.createdAuthorDto = createdAuthorDto;
    }
}
