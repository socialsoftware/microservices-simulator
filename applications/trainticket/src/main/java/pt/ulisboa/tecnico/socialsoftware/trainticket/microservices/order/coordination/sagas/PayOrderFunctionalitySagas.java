package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.GetOrderByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.PayOrderCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.states.OrderSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class PayOrderFunctionalitySagas extends WorkflowFunctionality {
    private OrderDto orderDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public PayOrderFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderAggregateId,
                                 SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(orderAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer orderAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getOrderStep = new SagaStep("getOrderStep", () -> {
            GetOrderByIdCommand readCmd = new GetOrderByIdCommand(
                    unitOfWork, ServiceMapping.ORDER.getServiceName(), orderAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(OrderSagaState.IN_PAY_ORDER);
            this.orderDto = (OrderDto) commandGateway.send(sagaCommand);
        });

        SagaStep payOrderStep = new SagaStep("payOrderStep", () -> {
            PayOrderCommand cmd = new PayOrderCommand(
                    unitOfWork, ServiceMapping.ORDER.getServiceName(), orderAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getOrderStep)));

        this.workflow.addStep(getOrderStep);
        this.workflow.addStep(payOrderStep);
    }

    public OrderDto getOrderDto() {
        return orderDto;
    }
}
