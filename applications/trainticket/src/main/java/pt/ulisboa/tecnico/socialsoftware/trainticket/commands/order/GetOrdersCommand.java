package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetOrdersCommand extends Command {
    public GetOrdersCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }
}
