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

public class UpdateCartItemFunctionalitySagas extends WorkflowFunctionality {
    private CartItemDto updatedItemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateCartItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer cartId, Integer quantity, CartItemDto itemDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(cartId, quantity, itemDto, unitOfWork);
    }

    public void buildWorkflow(Integer cartId, Integer quantity, CartItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateItemStep = new SagaStep("updateItemStep", () -> {
            UpdateCartItemCommand cmd = new UpdateCartItemCommand(unitOfWork, ServiceMapping.CART.getServiceName(), cartId, quantity, itemDto);
            CartItemDto updatedItemDto = (CartItemDto) commandGateway.send(cmd);
            setUpdatedItemDto(updatedItemDto);
        });

        workflow.addStep(updateItemStep);
    }
    public CartItemDto getUpdatedItemDto() {
        return updatedItemDto;
    }

    public void setUpdatedItemDto(CartItemDto updatedItemDto) {
        this.updatedItemDto = updatedItemDto;
    }
}
