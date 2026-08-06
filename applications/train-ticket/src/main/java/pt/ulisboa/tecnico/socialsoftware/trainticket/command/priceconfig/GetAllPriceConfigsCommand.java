package pt.ulisboa.tecnico.socialsoftware.trainticket.command.priceconfig;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class GetAllPriceConfigsCommand extends Command {


    public GetAllPriceConfigsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);

    }


}
