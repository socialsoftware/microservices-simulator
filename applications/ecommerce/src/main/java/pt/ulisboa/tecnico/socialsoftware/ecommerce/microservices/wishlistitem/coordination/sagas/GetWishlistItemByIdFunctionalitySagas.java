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

public class GetWishlistItemByIdFunctionalitySagas extends WorkflowFunctionality {
    private WishlistItemDto wishlistitemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetWishlistItemByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer wishlistitemAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(wishlistitemAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer wishlistitemAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getWishlistItemStep = new SagaStep("getWishlistItemStep", () -> {
            GetWishlistItemByIdCommand cmd = new GetWishlistItemByIdCommand(unitOfWork, ServiceMapping.WISHLIST_ITEM.getServiceName(), wishlistitemAggregateId);
            WishlistItemDto wishlistitemDto = (WishlistItemDto) commandGateway.send(cmd);
            setWishlistItemDto(wishlistitemDto);
        });

        workflow.addStep(getWishlistItemStep);
    }
    public WishlistItemDto getWishlistItemDto() {
        return wishlistitemDto;
    }

    public void setWishlistItemDto(WishlistItemDto wishlistitemDto) {
        this.wishlistitemDto = wishlistitemDto;
    }
}
