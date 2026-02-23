package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.service.CartService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateCartFunctionalitySagas extends WorkflowFunctionality {
    private CartDto updatedCartDto;
    private final CartService cartService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateCartFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CartService cartService, CartDto cartDto) {
        this.cartService = cartService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(cartDto, unitOfWork);
    }

    public void buildWorkflow(CartDto cartDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateCartStep = new SagaSyncStep("updateCartStep", () -> {
            CartDto updatedCartDto = cartService.updateCart(cartDto, unitOfWork);
            setUpdatedCartDto(updatedCartDto);
        });

        workflow.addStep(updateCartStep);
    }
    public CartDto getUpdatedCartDto() {
        return updatedCartDto;
    }

    public void setUpdatedCartDto(CartDto updatedCartDto) {
        this.updatedCartDto = updatedCartDto;
    }
}
