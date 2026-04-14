package pt.ulisboa.tecnico.socialsoftware.advanced.command.product;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetAllProductsCommand extends Command {


    public GetAllProductsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);

    }


}
