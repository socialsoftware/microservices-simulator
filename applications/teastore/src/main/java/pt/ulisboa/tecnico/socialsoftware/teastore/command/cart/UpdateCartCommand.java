package pt.ulisboa.tecnico.socialsoftware.teastore.command.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;

public class UpdateCartCommand extends Command {
    private final CartDto cartDto;

    public UpdateCartCommand(UnitOfWork unitOfWork, String serviceName, CartDto cartDto) {
        super(unitOfWork, serviceName, null);
        this.cartDto = cartDto;
    }

    public CartDto getCartDto() { return cartDto; }
}
