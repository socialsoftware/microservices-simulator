package pt.ulisboa.tecnico.socialsoftware.advanced.command.customer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetAllCustomersCommand extends Command {


    public GetAllCustomersCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);

    }


}
