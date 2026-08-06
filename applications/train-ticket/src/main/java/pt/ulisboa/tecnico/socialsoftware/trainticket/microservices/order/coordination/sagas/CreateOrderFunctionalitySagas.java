package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;

public class CreateOrderFunctionalitySagas extends WorkflowFunctionality {
    private OrderDto createdOrderDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateOrderFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateOrderRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateOrderRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createOrderStep = new SagaStep("createOrderStep", () -> {
            CreateOrderCommand cmd = new CreateOrderCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), createRequest);
            OrderDto createdOrderDto = (OrderDto) commandGateway.send(cmd);
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
