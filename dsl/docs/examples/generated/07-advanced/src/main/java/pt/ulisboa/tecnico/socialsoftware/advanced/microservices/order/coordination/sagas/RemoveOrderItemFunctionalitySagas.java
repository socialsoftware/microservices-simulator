package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveOrderItemFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveOrderItemFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderId, Integer key, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderId, key, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer key, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep removeItemStep = new SagaStep("removeItemStep", () -> {
            RemoveOrderItemCommand cmd = new RemoveOrderItemCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderId, key);
            commandGateway.send(cmd);
        });

        workflow.addStep(removeItemStep);
    }
}
