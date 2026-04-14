package pt.ulisboa.tecnico.socialsoftware.advanced.command.invoice;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.webapi.requestDtos.CreateInvoiceRequestDto;

public class CreateInvoiceCommand extends Command {
    private final CreateInvoiceRequestDto createRequest;

    public CreateInvoiceCommand(UnitOfWork unitOfWork, String serviceName, CreateInvoiceRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateInvoiceRequestDto getCreateRequest() { return createRequest; }
}
