package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.invoice.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteInvoiceFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteInvoiceFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer invoiceAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(invoiceAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer invoiceAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteInvoiceStep = new SagaStep("deleteInvoiceStep", () -> {
            DeleteInvoiceCommand cmd = new DeleteInvoiceCommand(unitOfWork, ServiceMapping.INVOICE.getServiceName(), invoiceAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteInvoiceStep);
    }
}
