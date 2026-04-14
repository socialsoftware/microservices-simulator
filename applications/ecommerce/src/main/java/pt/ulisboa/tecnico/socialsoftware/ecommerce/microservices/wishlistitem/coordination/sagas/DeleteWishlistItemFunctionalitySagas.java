package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.wishlistitem.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteWishlistItemFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteWishlistItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer wishlistitemAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(wishlistitemAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer wishlistitemAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteWishlistItemStep = new SagaStep("deleteWishlistItemStep", () -> {
            DeleteWishlistItemCommand cmd = new DeleteWishlistItemCommand(unitOfWork, ServiceMapping.WISHLIST_ITEM.getServiceName(), wishlistitemAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteWishlistItemStep);
    }
}
