package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddOrderItemsFunctionalitySagas extends WorkflowFunctionality {
    private List<OrderItemDto> addedItemDtos;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddOrderItemsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, Integer orderId, List<OrderItemDto> itemDtos) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderId, itemDtos, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, List<OrderItemDto> itemDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addItemsStep = new SagaSyncStep("addItemsStep", () -> {
            List<OrderItemDto> addedItemDtos = orderService.addOrderItems(orderId, itemDtos, unitOfWork);
            setAddedItemDtos(addedItemDtos);
        });

        workflow.addStep(addItemsStep);
    }
    public List<OrderItemDto> getAddedItemDtos() {
        return addedItemDtos;
    }

    public void setAddedItemDtos(List<OrderItemDto> addedItemDtos) {
        this.addedItemDtos = addedItemDtos;
    }
}
