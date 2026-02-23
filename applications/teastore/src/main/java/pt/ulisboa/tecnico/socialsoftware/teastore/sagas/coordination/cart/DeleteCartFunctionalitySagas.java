package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.service.CartService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteCartFunctionalitySagas extends WorkflowFunctionality {
    private final CartService cartService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteCartFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CartService cartService, Integer cartAggregateId) {
        this.cartService = cartService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(cartAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer cartAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteCartStep = new SagaSyncStep("deleteCartStep", () -> {
            cartService.deleteCart(cartAggregateId, unitOfWork);
        });

        workflow.addStep(deleteCartStep);
    }
}
