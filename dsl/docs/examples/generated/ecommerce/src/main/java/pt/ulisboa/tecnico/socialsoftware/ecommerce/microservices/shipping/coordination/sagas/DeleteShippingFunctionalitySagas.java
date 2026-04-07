package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.shipping.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteShippingFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteShippingFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer shippingAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(shippingAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer shippingAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteShippingStep = new SagaStep("deleteShippingStep", () -> {
            DeleteShippingCommand cmd = new DeleteShippingCommand(unitOfWork, ServiceMapping.SHIPPING.getServiceName(), shippingAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteShippingStep);
    }
}
