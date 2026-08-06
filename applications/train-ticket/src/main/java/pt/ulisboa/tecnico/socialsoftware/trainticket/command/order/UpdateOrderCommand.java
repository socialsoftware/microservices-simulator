package pt.ulisboa.tecnico.socialsoftware.trainticket.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderDto;

public class UpdateOrderCommand extends Command {
    private final OrderDto orderDto;

    public UpdateOrderCommand(UnitOfWork unitOfWork, String serviceName, OrderDto orderDto) {
        super(unitOfWork, serviceName, null);
        this.orderDto = orderDto;
    }

    public OrderDto getOrderDto() { return orderDto; }
}
