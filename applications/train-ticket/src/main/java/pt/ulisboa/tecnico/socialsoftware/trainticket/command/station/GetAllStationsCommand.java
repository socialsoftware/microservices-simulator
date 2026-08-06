package pt.ulisboa.tecnico.socialsoftware.trainticket.command.station;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class GetAllStationsCommand extends Command {


    public GetAllStationsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);

    }


}
