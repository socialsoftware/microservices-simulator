package pt.ulisboa.tecnico.socialsoftware.advanced.command.invoice;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetAllInvoicesCommand extends Command {


    public GetAllInvoicesCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);

    }


}
