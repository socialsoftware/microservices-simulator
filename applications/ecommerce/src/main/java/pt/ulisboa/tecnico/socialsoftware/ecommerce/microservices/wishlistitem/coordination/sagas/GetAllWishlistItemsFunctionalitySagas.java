package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.wishlistitem.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllWishlistItemsFunctionalitySagas extends WorkflowFunctionality {
    private List<WishlistItemDto> wishlistitems;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllWishlistItemsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllWishlistItemsStep = new SagaStep("getAllWishlistItemsStep", () -> {
            GetAllWishlistItemsCommand cmd = new GetAllWishlistItemsCommand(unitOfWork, ServiceMapping.WISHLIST_ITEM.getServiceName());
            List<WishlistItemDto> wishlistitems = (List<WishlistItemDto>) commandGateway.send(cmd);
            setWishlistItems(wishlistitems);
        });

        workflow.addStep(getAllWishlistItemsStep);
    }
    public List<WishlistItemDto> getWishlistItems() {
        return wishlistitems;
    }

    public void setWishlistItems(List<WishlistItemDto> wishlistitems) {
        this.wishlistitems = wishlistitems;
    }
}
