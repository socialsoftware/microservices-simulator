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

public class UpdateWishlistItemFunctionalitySagas extends WorkflowFunctionality {
    private WishlistItemDto updatedWishlistItemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateWishlistItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, WishlistItemDto wishlistitemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(wishlistitemDto, unitOfWork);
    }

    public void buildWorkflow(WishlistItemDto wishlistitemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateWishlistItemStep = new SagaStep("updateWishlistItemStep", () -> {
            UpdateWishlistItemCommand cmd = new UpdateWishlistItemCommand(unitOfWork, ServiceMapping.WISHLIST_ITEM.getServiceName(), wishlistitemDto);
            WishlistItemDto updatedWishlistItemDto = (WishlistItemDto) commandGateway.send(cmd);
            setUpdatedWishlistItemDto(updatedWishlistItemDto);
        });

        workflow.addStep(updateWishlistItemStep);
    }
    public WishlistItemDto getUpdatedWishlistItemDto() {
        return updatedWishlistItemDto;
    }

    public void setUpdatedWishlistItemDto(WishlistItemDto updatedWishlistItemDto) {
        this.updatedWishlistItemDto = updatedWishlistItemDto;
    }
}
