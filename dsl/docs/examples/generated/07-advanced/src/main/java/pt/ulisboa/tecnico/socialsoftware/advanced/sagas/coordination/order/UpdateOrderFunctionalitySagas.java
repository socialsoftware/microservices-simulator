package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateOrderFunctionalitySagas extends WorkflowFunctionality {
    private OrderDto updatedOrderDto;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateOrderFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, OrderDto orderDto) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderDto, unitOfWork);
    }

    public void buildWorkflow(OrderDto orderDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateOrderStep = new SagaSyncStep("updateOrderStep", () -> {
            OrderDto updatedOrderDto = orderService.updateOrder(orderDto, unitOfWork);
            setUpdatedOrderDto(updatedOrderDto);
        });

        workflow.addStep(updateOrderStep);
    }
    public OrderDto getUpdatedOrderDto() {
        return updatedOrderDto;
    }

    public void setUpdatedOrderDto(OrderDto updatedOrderDto) {
        this.updatedOrderDto = updatedOrderDto;
    }
}
