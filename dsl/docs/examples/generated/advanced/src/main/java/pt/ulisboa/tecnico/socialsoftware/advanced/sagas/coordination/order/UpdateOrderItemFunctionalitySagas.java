package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateOrderItemFunctionalitySagas extends WorkflowFunctionality {
    private OrderItemDto updatedItemDto;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateOrderItemFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, Integer orderId, Integer key, OrderItemDto itemDto) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderId, key, itemDto, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer key, OrderItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateItemStep = new SagaSyncStep("updateItemStep", () -> {
            OrderItemDto updatedItemDto = orderService.updateOrderItem(orderId, key, itemDto, unitOfWork);
            setUpdatedItemDto(updatedItemDto);
        });

        workflow.addStep(updateItemStep);
    }
    public OrderItemDto getUpdatedItemDto() {
        return updatedItemDto;
    }

    public void setUpdatedItemDto(OrderItemDto updatedItemDto) {
        this.updatedItemDto = updatedItemDto;
    }
}
