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
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.webapi.requestDtos.CreateWishlistItemRequestDto;

public class CreateWishlistItemFunctionalitySagas extends WorkflowFunctionality {
    private WishlistItemDto createdWishlistItemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateWishlistItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateWishlistItemRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateWishlistItemRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createWishlistItemStep = new SagaStep("createWishlistItemStep", () -> {
            CreateWishlistItemCommand cmd = new CreateWishlistItemCommand(unitOfWork, ServiceMapping.WISHLIST_ITEM.getServiceName(), createRequest);
            WishlistItemDto createdWishlistItemDto = (WishlistItemDto) commandGateway.send(cmd);
            setCreatedWishlistItemDto(createdWishlistItemDto);
        });

        workflow.addStep(createWishlistItemStep);
    }
    public WishlistItemDto getCreatedWishlistItemDto() {
        return createdWishlistItemDto;
    }

    public void setCreatedWishlistItemDto(WishlistItemDto createdWishlistItemDto) {
        this.createdWishlistItemDto = createdWishlistItemDto;
    }
}
