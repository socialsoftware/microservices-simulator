package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.invoice.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateInvoiceFunctionalitySagas extends WorkflowFunctionality {
    private InvoiceDto updatedInvoiceDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateInvoiceFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, InvoiceDto invoiceDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(invoiceDto, unitOfWork);
    }

    public void buildWorkflow(InvoiceDto invoiceDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateInvoiceStep = new SagaStep("updateInvoiceStep", () -> {
            UpdateInvoiceCommand cmd = new UpdateInvoiceCommand(unitOfWork, ServiceMapping.INVOICE.getServiceName(), invoiceDto);
            InvoiceDto updatedInvoiceDto = (InvoiceDto) commandGateway.send(cmd);
            setUpdatedInvoiceDto(updatedInvoiceDto);
        });

        workflow.addStep(updateInvoiceStep);
    }
    public InvoiceDto getUpdatedInvoiceDto() {
        return updatedInvoiceDto;
    }

    public void setUpdatedInvoiceDto(InvoiceDto updatedInvoiceDto) {
        this.updatedInvoiceDto = updatedInvoiceDto;
    }
}
