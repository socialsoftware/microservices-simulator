package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.invoice;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.service.InvoiceService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateInvoiceFunctionalitySagas extends WorkflowFunctionality {
    private InvoiceDto updatedInvoiceDto;
    private final InvoiceService invoiceService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateInvoiceFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, InvoiceService invoiceService, InvoiceDto invoiceDto) {
        this.invoiceService = invoiceService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(invoiceDto, unitOfWork);
    }

    public void buildWorkflow(InvoiceDto invoiceDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateInvoiceStep = new SagaSyncStep("updateInvoiceStep", () -> {
            InvoiceDto updatedInvoiceDto = invoiceService.updateInvoice(invoiceDto, unitOfWork);
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
