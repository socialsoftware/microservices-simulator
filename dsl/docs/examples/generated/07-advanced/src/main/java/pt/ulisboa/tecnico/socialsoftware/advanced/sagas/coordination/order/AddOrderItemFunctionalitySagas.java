package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddOrderItemFunctionalitySagas extends WorkflowFunctionality {
    private OrderItemDto addedItemDto;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddOrderItemFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, Integer orderId, Integer key, OrderItemDto itemDto) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderId, key, itemDto, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer key, OrderItemDto itemDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addItemStep = new SagaSyncStep("addItemStep", () -> {
            OrderItemDto addedItemDto = orderService.addOrderItem(orderId, key, itemDto, unitOfWork);
            setAddedItemDto(addedItemDto);
        });

        workflow.addStep(addItemStep);
    }
    public OrderItemDto getAddedItemDto() {
        return addedItemDto;
    }

    public void setAddedItemDto(OrderItemDto addedItemDto) {
        this.addedItemDto = addedItemDto;
    }
}
