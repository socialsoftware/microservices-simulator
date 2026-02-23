package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.post;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service.PostService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllPostsFunctionalitySagas extends WorkflowFunctionality {
    private List<PostDto> posts;
    private final PostService postService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllPostsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, PostService postService) {
        this.postService = postService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllPostsStep = new SagaSyncStep("getAllPostsStep", () -> {
            List<PostDto> posts = postService.getAllPosts(unitOfWork);
            setPosts(posts);
        });

        workflow.addStep(getAllPostsStep);
    }
    public List<PostDto> getPosts() {
        return posts;
    }

    public void setPosts(List<PostDto> posts) {
        this.posts = posts;
    }
}
