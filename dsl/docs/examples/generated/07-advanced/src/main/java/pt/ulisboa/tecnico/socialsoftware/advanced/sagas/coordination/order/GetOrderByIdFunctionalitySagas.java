package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetOrderByIdFunctionalitySagas extends WorkflowFunctionality {
    private OrderDto orderDto;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetOrderByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, Integer orderAggregateId) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer orderAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getOrderStep = new SagaSyncStep("getOrderStep", () -> {
            OrderDto orderDto = orderService.getOrderById(orderAggregateId, unitOfWork);
            setOrderDto(orderDto);
        });

        workflow.addStep(getOrderStep);
    }
    public OrderDto getOrderDto() {
        return orderDto;
    }

    public void setOrderDto(OrderDto orderDto) {
        this.orderDto = orderDto;
    }
}
