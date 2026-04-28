package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.sagas.states.OrderSagaState;

public class DeleteOrderFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteOrderFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer orderAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteOrderStep = new SagaStep("deleteOrderStep", () -> {
            unitOfWorkService.verifySagaState(orderAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(OrderSagaState.READ_ORDER, OrderSagaState.UPDATE_ORDER, OrderSagaState.DELETE_ORDER)));
            unitOfWorkService.registerSagaState(orderAggregateId, OrderSagaState.DELETE_ORDER, unitOfWork);
            DeleteOrderCommand cmd = new DeleteOrderCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteOrderStep);
    }
}
