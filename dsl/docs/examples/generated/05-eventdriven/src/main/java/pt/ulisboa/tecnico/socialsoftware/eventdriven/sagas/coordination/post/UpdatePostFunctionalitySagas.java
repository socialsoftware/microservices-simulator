package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.post;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service.PostService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdatePostFunctionalitySagas extends WorkflowFunctionality {
    private PostDto updatedPostDto;
    private final PostService postService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdatePostFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, PostService postService, PostDto postDto) {
        this.postService = postService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(postDto, unitOfWork);
    }

    public void buildWorkflow(PostDto postDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updatePostStep = new SagaSyncStep("updatePostStep", () -> {
            PostDto updatedPostDto = postService.updatePost(postDto, unitOfWork);
            setUpdatedPostDto(updatedPostDto);
        });

        workflow.addStep(updatePostStep);
    }
    public PostDto getUpdatedPostDto() {
        return updatedPostDto;
    }

    public void setUpdatedPostDto(PostDto updatedPostDto) {
        this.updatedPostDto = updatedPostDto;
    }
}
