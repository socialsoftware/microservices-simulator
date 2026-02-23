package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.post;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service.PostService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeletePostFunctionalitySagas extends WorkflowFunctionality {
    private final PostService postService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeletePostFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, PostService postService, Integer postAggregateId) {
        this.postService = postService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(postAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer postAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deletePostStep = new SagaSyncStep("deletePostStep", () -> {
            postService.deletePost(postAggregateId, unitOfWork);
        });

        workflow.addStep(deletePostStep);
    }
}
