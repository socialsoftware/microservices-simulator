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

public class GetCartItemFunctionalitySagas extends WorkflowFunctionality {
    private CartItemDto itemDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetCartItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer cartId, Integer quantity, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(cartId, quantity, unitOfWork);
    }

    public void buildWorkflow(Integer cartId, Integer quantity, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getItemStep = new SagaStep("getItemStep", () -> {
            GetCartItemCommand cmd = new GetCartItemCommand(unitOfWork, ServiceMapping.CART.getServiceName(), cartId, quantity);
            CartItemDto itemDto = (CartItemDto) commandGateway.send(cmd);
            setItemDto(itemDto);
        });

        workflow.addStep(getItemStep);
    }
    public CartItemDto getItemDto() {
        return itemDto;
    }

    public void setItemDto(CartItemDto itemDto) {
        this.itemDto = itemDto;
    }
}
