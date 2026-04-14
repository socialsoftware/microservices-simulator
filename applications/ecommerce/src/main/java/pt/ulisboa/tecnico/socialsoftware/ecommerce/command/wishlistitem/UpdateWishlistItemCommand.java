package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.wishlistitem;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;

public class UpdateWishlistItemCommand extends Command {
    private final WishlistItemDto wishlistitemDto;

    public UpdateWishlistItemCommand(UnitOfWork unitOfWork, String serviceName, WishlistItemDto wishlistitemDto) {
        super(unitOfWork, serviceName, null);
        this.wishlistitemDto = wishlistitemDto;
    }

    public WishlistItemDto getWishlistitemDto() { return wishlistitemDto; }
}
