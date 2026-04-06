package pt.ulisboa.tecnico.socialsoftware.teastore.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;

public class UpdateOrderCommand extends Command {
    private final OrderDto orderDto;

    public UpdateOrderCommand(UnitOfWork unitOfWork, String serviceName, OrderDto orderDto) {
        super(unitOfWork, serviceName, null);
        this.orderDto = orderDto;
    }

    public OrderDto getOrderDto() { return orderDto; }
}
