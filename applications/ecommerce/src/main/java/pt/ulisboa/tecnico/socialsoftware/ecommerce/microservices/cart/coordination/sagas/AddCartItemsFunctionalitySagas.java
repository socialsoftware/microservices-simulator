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
import java.util.List;

public class AddCartItemsFunctionalitySagas extends WorkflowFunctionality {
    private List<CartItemDto> addedItemDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddCartItemsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer cartId, List<CartItemDto> itemDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(cartId, itemDtos, unitOfWork);
    }

    public void buildWorkflow(Integer cartId, List<CartItemDto> itemDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addItemsStep = new SagaStep("addItemsStep", () -> {
            AddCartItemsCommand cmd = new AddCartItemsCommand(unitOfWork, ServiceMapping.CART.getServiceName(), cartId, itemDtos);
            List<CartItemDto> addedItemDtos = (List<CartItemDto>) commandGateway.send(cmd);
            setAddedItemDtos(addedItemDtos);
        });

        workflow.addStep(addItemsStep);
    }
    public List<CartItemDto> getAddedItemDtos() {
        return addedItemDtos;
    }

    public void setAddedItemDtos(List<CartItemDto> addedItemDtos) {
        this.addedItemDtos = addedItemDtos;
    }
}
