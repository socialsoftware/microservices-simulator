package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetPriceConfigsCommand extends Command {
    public GetPriceConfigsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }
}
