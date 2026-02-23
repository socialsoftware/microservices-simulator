package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetOrderItemFunctionalitySagas extends WorkflowFunctionality {
    private OrderItemDto itemDto;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetOrderItemFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, Integer orderId, Integer key) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderId, key, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer key, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getItemStep = new SagaSyncStep("getItemStep", () -> {
            OrderItemDto itemDto = orderService.getOrderItem(orderId, key, unitOfWork);
            setItemDto(itemDto);
        });

        workflow.addStep(getItemStep);
    }
    public OrderItemDto getItemDto() {
        return itemDto;
    }

    public void setItemDto(OrderItemDto itemDto) {
        this.itemDto = itemDto;
    }
}
