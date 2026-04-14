package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveCartItemCommand extends Command {
    private final Integer cartId;
    private final Integer quantity;

    public RemoveCartItemCommand(UnitOfWork unitOfWork, String serviceName, Integer cartId, Integer quantity) {
        super(unitOfWork, serviceName, null);
        this.cartId = cartId;
        this.quantity = quantity;
    }

    public Integer getCartId() { return cartId; }
    public Integer getQuantity() { return quantity; }
}
