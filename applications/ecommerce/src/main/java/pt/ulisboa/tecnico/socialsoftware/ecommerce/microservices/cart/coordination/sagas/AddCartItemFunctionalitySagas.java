package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.cart.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartItemDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddCartItemFunctionalitySagas extends WorkflowFunctionality {
    private CartItemDto addedItemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddCartItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer cartId, Integer quantity, CartItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(cartId, quantity, itemDto, unitOfWork);
    }

    public void buildWorkflow(Integer cartId, Integer quantity, CartItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addItemStep = new SagaStep("addItemStep", () -> {
            AddCartItemCommand cmd = new AddCartItemCommand(unitOfWork, ServiceMapping.CART.getServiceName(), cartId, quantity, itemDto);
            CartItemDto addedItemDto = (CartItemDto) commandGateway.send(cmd);
            setAddedItemDto(addedItemDto);
        });

        workflow.addStep(addItemStep);
    }
    public CartItemDto getAddedItemDto() {
        return addedItemDto;
    }

    public void setAddedItemDto(CartItemDto addedItemDto) {
        this.addedItemDto = addedItemDto;
    }
}
