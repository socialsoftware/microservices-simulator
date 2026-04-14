package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.wishlistitem;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetAllWishlistItemsCommand extends Command {


    public GetAllWishlistItemsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);

    }


}
