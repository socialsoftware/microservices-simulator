package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.command.post.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.sagas.states.PostSagaState;

public class DeletePostFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeletePostFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer postAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(postAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer postAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deletePostStep = new SagaStep("deletePostStep", () -> {
            unitOfWorkService.verifySagaState(postAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(PostSagaState.READ_POST, PostSagaState.UPDATE_POST, PostSagaState.DELETE_POST)));
            unitOfWorkService.registerSagaState(postAggregateId, PostSagaState.DELETE_POST, unitOfWork);
            DeletePostCommand cmd = new DeletePostCommand(unitOfWork, ServiceMapping.POST.getServiceName(), postAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deletePostStep);
    }
}
