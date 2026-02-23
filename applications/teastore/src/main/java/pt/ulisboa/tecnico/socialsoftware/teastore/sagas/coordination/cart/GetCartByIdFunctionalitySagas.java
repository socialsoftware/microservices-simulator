package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.service.CartService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetCartByIdFunctionalitySagas extends WorkflowFunctionality {
    private CartDto cartDto;
    private final CartService cartService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetCartByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CartService cartService, Integer cartAggregateId) {
        this.cartService = cartService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(cartAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer cartAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCartStep = new SagaSyncStep("getCartStep", () -> {
            CartDto cartDto = cartService.getCartById(cartAggregateId, unitOfWork);
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
