package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.GetOrdersByAccountCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;

import java.util.List;

public class GetOrdersByAccountFunctionalitySagas extends WorkflowFunctionality {
    private List<OrderDto> orders;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetOrdersByAccountFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer userAggregateId,
                                                SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(userAggregateId, unitOfWork);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getOrdersStep = new SagaStep("getOrdersStep", () -> {
            GetOrdersByAccountCommand cmd = new GetOrdersByAccountCommand(
                    unitOfWork, ServiceMapping.ORDER.getServiceName(), userAggregateId);
            this.orders = (List<OrderDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getOrdersStep);
    }

    public List<OrderDto> getOrders() {
        return orders;
    }
}
