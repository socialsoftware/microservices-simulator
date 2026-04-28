package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.sagas.states.OrderSagaState;

public class GetOrderByIdFunctionalitySagas extends WorkflowFunctionality {
    private OrderDto orderDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetOrderByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer orderAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getOrderStep = new SagaStep("getOrderStep", () -> {
            unitOfWorkService.verifySagaState(orderAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(OrderSagaState.UPDATE_ORDER, OrderSagaState.DELETE_ORDER)));
            unitOfWorkService.registerSagaState(orderAggregateId, OrderSagaState.READ_ORDER, unitOfWork);
            GetOrderByIdCommand cmd = new GetOrderByIdCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderAggregateId);
            OrderDto orderDto = (OrderDto) commandGateway.send(cmd);
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
