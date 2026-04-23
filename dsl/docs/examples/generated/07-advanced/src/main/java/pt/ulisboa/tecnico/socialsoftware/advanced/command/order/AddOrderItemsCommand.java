package pt.ulisboa.tecnico.socialsoftware.advanced.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;
import java.util.List;

public class AddOrderItemsCommand extends Command {
    private final Integer orderId;
    private final List<OrderItemDto> itemDtos;

    public AddOrderItemsCommand(UnitOfWork unitOfWork, String serviceName, Integer orderId, List<OrderItemDto> itemDtos) {
        super(unitOfWork, serviceName, null);
        this.orderId = orderId;
        this.itemDtos = itemDtos;
    }

    public Integer getOrderId() { return orderId; }
    public List<OrderItemDto> getItemDtos() { return itemDtos; }
}
