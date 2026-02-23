package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.invoice;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.service.InvoiceService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllInvoicesFunctionalitySagas extends WorkflowFunctionality {
    private List<InvoiceDto> invoices;
    private final InvoiceService invoiceService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllInvoicesFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllInvoicesStep = new SagaSyncStep("getAllInvoicesStep", () -> {
            List<InvoiceDto> invoices = invoiceService.getAllInvoices(unitOfWork);
            setInvoices(invoices);
        });

        workflow.addStep(getAllInvoicesStep);
    }
    public List<InvoiceDto> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<InvoiceDto> invoices) {
        this.invoices = invoices;
    }
}
