package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.cart.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.sagas.states.CartSagaState;

public class GetCartByIdFunctionalitySagas extends WorkflowFunctionality {
    private CartDto cartDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetCartByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer cartAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(cartAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer cartAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCartStep = new SagaStep("getCartStep", () -> {
            unitOfWorkService.verifySagaState(cartAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(CartSagaState.UPDATE_CART, CartSagaState.DELETE_CART)));
            unitOfWorkService.registerSagaState(cartAggregateId, CartSagaState.READ_CART, unitOfWork);
            GetCartByIdCommand cmd = new GetCartByIdCommand(unitOfWork, ServiceMapping.CART.getServiceName(), cartAggregateId);
            CartDto cartDto = (CartDto) commandGateway.send(cmd);
            setCartDto(cartDto);
        });

        workflow.addStep(getCartStep);
    }
    public CartDto getCartDto() {
        return cartDto;
    }

    public void setCartDto(CartDto cartDto) {
        this.cartDto = cartDto;
    }
}
