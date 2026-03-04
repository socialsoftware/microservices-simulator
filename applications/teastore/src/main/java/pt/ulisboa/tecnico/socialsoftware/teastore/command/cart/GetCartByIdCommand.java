package pt.ulisboa.tecnico.socialsoftware.teastore.command.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class GetCartByIdCommand extends Command {


    public GetCartByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer aggregateId) {
        super(unitOfWork, serviceName, aggregateId);

    }


}
