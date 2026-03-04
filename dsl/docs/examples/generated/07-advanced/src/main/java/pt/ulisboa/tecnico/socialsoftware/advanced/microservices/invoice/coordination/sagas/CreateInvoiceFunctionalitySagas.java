package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.invoice.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.webapi.requestDtos.CreateInvoiceRequestDto;

public class CreateInvoiceFunctionalitySagas extends WorkflowFunctionality {
    private InvoiceDto createdInvoiceDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateInvoiceFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateInvoiceRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateInvoiceRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createInvoiceStep = new SagaStep("createInvoiceStep", () -> {
            CreateInvoiceCommand cmd = new CreateInvoiceCommand(unitOfWork, ServiceMapping.INVOICE.getServiceName(), createRequest);
            InvoiceDto createdInvoiceDto = (InvoiceDto) commandGateway.send(cmd);
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
