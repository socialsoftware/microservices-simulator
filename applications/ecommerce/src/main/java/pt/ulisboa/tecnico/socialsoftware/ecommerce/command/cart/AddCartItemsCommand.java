package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.cart;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartItemDto;
import java.util.List;

public class AddCartItemsCommand extends Command {
    private final Integer cartId;
    private final List<CartItemDto> itemDtos;

    public AddCartItemsCommand(UnitOfWork unitOfWork, String serviceName, Integer cartId, List<CartItemDto> itemDtos) {
        super(unitOfWork, serviceName, null);
        this.cartId = cartId;
        this.itemDtos = itemDtos;
    }

    public Integer getCartId() { return cartId; }
    public List<CartItemDto> getItemDtos() { return itemDtos; }
}
