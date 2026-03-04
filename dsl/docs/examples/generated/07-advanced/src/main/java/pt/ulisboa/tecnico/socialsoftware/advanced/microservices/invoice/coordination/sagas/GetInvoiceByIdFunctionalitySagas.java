package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.invoice.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetInvoiceByIdFunctionalitySagas extends WorkflowFunctionality {
    private InvoiceDto invoiceDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetInvoiceByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer invoiceAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(invoiceAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer invoiceAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getInvoiceStep = new SagaStep("getInvoiceStep", () -> {
            GetInvoiceByIdCommand cmd = new GetInvoiceByIdCommand(unitOfWork, ServiceMapping.INVOICE.getServiceName(), invoiceAggregateId);
            InvoiceDto invoiceDto = (InvoiceDto) commandGateway.send(cmd);
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
