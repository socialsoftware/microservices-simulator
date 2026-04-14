package pt.ulisboa.tecnico.socialsoftware.advanced.command.invoice;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class DeleteInvoiceCommand extends Command {


    public DeleteInvoiceCommand(UnitOfWork unitOfWork, String serviceName, Integer aggregateId) {
        super(unitOfWork, serviceName, aggregateId);

    }


}
