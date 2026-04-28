package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.command.post.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.sagas.states.PostSagaState;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.coordination.webapi.requestDtos.CreatePostRequestDto;

public class CreatePostFunctionalitySagas extends WorkflowFunctionality {
    private PostDto createdPostDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreatePostFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreatePostRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreatePostRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createPostStep = new SagaStep("createPostStep", () -> {
            CreatePostCommand cmd = new CreatePostCommand(unitOfWork, ServiceMapping.POST.getServiceName(), createRequest);
            PostDto createdPostDto = (PostDto) commandGateway.send(cmd);
            setCreatedPostDto(createdPostDto);
        });

        workflow.addStep(createPostStep);
    }
    public PostDto getCreatedPostDto() {
        return createdPostDto;
    }

    public void setCreatedPostDto(PostDto createdPostDto) {
        this.createdPostDto = createdPostDto;
    }
}
