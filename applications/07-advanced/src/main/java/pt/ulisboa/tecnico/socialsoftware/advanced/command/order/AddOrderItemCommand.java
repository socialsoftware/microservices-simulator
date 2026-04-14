package pt.ulisboa.tecnico.socialsoftware.advanced.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;

public class AddOrderItemCommand extends Command {
    private final Integer orderId;
    private final Integer key;
    private final OrderItemDto itemDto;

    public AddOrderItemCommand(UnitOfWork unitOfWork, String serviceName, Integer orderId, Integer key, OrderItemDto itemDto) {
        super(unitOfWork, serviceName, null);
        this.orderId = orderId;
        this.key = key;
        this.itemDto = itemDto;
    }

    public Integer getOrderId() { return orderId; }
    public Integer getKey() { return key; }
    public OrderItemDto getItemDto() { return itemDto; }
}
