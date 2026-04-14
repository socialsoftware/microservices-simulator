package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartItemDto;

public class UpdateCartItemCommand extends Command {
    private final Integer cartId;
    private final Integer quantity;
    private final CartItemDto itemDto;

    public UpdateCartItemCommand(UnitOfWork unitOfWork, String serviceName, Integer cartId, Integer quantity, CartItemDto itemDto) {
        super(unitOfWork, serviceName, null);
        this.cartId = cartId;
        this.quantity = quantity;
        this.itemDto = itemDto;
    }

    public Integer getCartId() { return cartId; }
    public Integer getQuantity() { return quantity; }
    public CartItemDto getItemDto() { return itemDto; }
}
