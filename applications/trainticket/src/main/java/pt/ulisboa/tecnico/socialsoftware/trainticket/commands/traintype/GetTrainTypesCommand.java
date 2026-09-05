package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetTrainTypesCommand extends Command {
    public GetTrainTypesCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }
}
