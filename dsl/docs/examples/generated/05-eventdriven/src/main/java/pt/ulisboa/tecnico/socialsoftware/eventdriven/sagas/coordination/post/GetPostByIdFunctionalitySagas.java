package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.post;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service.PostService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetPostByIdFunctionalitySagas extends WorkflowFunctionality {
    private PostDto postDto;
    private final PostService postService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetPostByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, PostService postService, Integer postAggregateId) {
        this.postService = postService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(postAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer postAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getPostStep = new SagaSyncStep("getPostStep", () -> {
            PostDto postDto = postService.getPostById(postAggregateId, unitOfWork);
            setPostDto(postDto);
        });

        workflow.addStep(getPostStep);
    }
    public PostDto getPostDto() {
        return postDto;
    }

    public void setPostDto(PostDto postDto) {
        this.postDto = postDto;
    }
}
