package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveOrderItemFunctionalitySagas extends WorkflowFunctionality {
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public RemoveOrderItemFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, Integer orderId, Integer key) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderId, key, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer key, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep removeItemStep = new SagaSyncStep("removeItemStep", () -> {
            orderService.removeOrderItem(orderId, key, unitOfWork);
        });

        workflow.addStep(removeItemStep);
    }
}
