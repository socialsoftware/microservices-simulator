package pt.ulisboa.tecnico.socialsoftware.trainticket.command.priceconfig;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetAllPriceConfigsCommand extends Command {


    public GetAllPriceConfigsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);

    }


}
