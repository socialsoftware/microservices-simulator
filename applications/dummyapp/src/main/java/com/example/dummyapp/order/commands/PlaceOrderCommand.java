package com.example.dummyapp.order.commands;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import com.example.dummyapp.order.aggregate.OrderDto;

public class PlaceOrderCommand extends Command {

    private final OrderDto orderDto;

    public PlaceOrderCommand(UnitOfWork unitOfWork, String serviceName, OrderDto orderDto) {
        super(unitOfWork, serviceName, null);
        this.orderDto = orderDto;
    }

    public OrderDto getOrderDto() { return orderDto; }
}
