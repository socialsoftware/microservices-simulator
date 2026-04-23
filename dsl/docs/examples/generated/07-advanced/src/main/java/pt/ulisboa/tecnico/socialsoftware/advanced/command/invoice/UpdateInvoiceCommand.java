package pt.ulisboa.tecnico.socialsoftware.advanced.command.invoice;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;

public class UpdateInvoiceCommand extends Command {
    private final InvoiceDto invoiceDto;

    public UpdateInvoiceCommand(UnitOfWork unitOfWork, String serviceName, InvoiceDto invoiceDto) {
        super(unitOfWork, serviceName, null);
        this.invoiceDto = invoiceDto;
    }

    public InvoiceDto getInvoiceDto() { return invoiceDto; }
}
