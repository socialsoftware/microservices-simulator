package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.post;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service.PostService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi.requestDtos.CreatePostRequestDto;

public class CreatePostFunctionalitySagas extends WorkflowFunctionality {
    private PostDto createdPostDto;
    private final PostService postService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreatePostFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, PostService postService, CreatePostRequestDto createRequest) {
        this.postService = postService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreatePostRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createPostStep = new SagaSyncStep("createPostStep", () -> {
            PostDto createdPostDto = postService.createPost(createRequest, unitOfWork);
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
