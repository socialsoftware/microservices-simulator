package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.wishlistitem;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.webapi.requestDtos.CreateWishlistItemRequestDto;

public class CreateWishlistItemCommand extends Command {
    private final CreateWishlistItemRequestDto createRequest;

    public CreateWishlistItemCommand(UnitOfWork unitOfWork, String serviceName, CreateWishlistItemRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateWishlistItemRequestDto getCreateRequest() { return createRequest; }
}
