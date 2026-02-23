package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi.requestDtos.CreateOrderRequestDto;

public class CreateOrderFunctionalitySagas extends WorkflowFunctionality {
    private OrderDto createdOrderDto;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateOrderFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, CreateOrderRequestDto createRequest) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateOrderRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createOrderStep = new SagaSyncStep("createOrderStep", () -> {
            OrderDto createdOrderDto = orderService.createOrder(createRequest, unitOfWork);
            setCreatedOrderDto(createdOrderDto);
        });

        workflow.addStep(createOrderStep);
    }
    public OrderDto getCreatedOrderDto() {
        return createdOrderDto;
    }

    public void setCreatedOrderDto(OrderDto createdOrderDto) {
        this.createdOrderDto = createdOrderDto;
    }
}
