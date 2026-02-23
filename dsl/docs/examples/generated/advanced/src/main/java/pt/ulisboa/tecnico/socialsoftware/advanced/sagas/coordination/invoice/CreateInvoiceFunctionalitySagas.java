package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.invoice;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.service.InvoiceService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi.requestDtos.CreateInvoiceRequestDto;

public class CreateInvoiceFunctionalitySagas extends WorkflowFunctionality {
    private InvoiceDto createdInvoiceDto;
    private final InvoiceService invoiceService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateInvoiceFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, InvoiceService invoiceService, CreateInvoiceRequestDto createRequest) {
        this.invoiceService = invoiceService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateInvoiceRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createInvoiceStep = new SagaSyncStep("createInvoiceStep", () -> {
            InvoiceDto createdInvoiceDto = invoiceService.createInvoice(createRequest, unitOfWork);
            setCreatedInvoiceDto(createdInvoiceDto);
        });

        workflow.addStep(createInvoiceStep);
    }
    public InvoiceDto getCreatedInvoiceDto() {
        return createdInvoiceDto;
    }

    public void setCreatedInvoiceDto(InvoiceDto createdInvoiceDto) {
        this.createdInvoiceDto = createdInvoiceDto;
    }
}
