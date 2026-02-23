package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.invoice;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.service.InvoiceService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetInvoiceByIdFunctionalitySagas extends WorkflowFunctionality {
    private InvoiceDto invoiceDto;
    private final InvoiceService invoiceService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetInvoiceByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, InvoiceService invoiceService, Integer invoiceAggregateId) {
        this.invoiceService = invoiceService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(invoiceAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer invoiceAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getInvoiceStep = new SagaSyncStep("getInvoiceStep", () -> {
            InvoiceDto invoiceDto = invoiceService.getInvoiceById(invoiceAggregateId, unitOfWork);
            setInvoiceDto(invoiceDto);
        });

        workflow.addStep(getInvoiceStep);
    }
    public InvoiceDto getInvoiceDto() {
        return invoiceDto;
    }

    public void setInvoiceDto(InvoiceDto invoiceDto) {
        this.invoiceDto = invoiceDto;
    }
}
