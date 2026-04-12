package com.example.dummyapp.order.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import com.example.dummyapp.order.commands.GetOrderCommand;
import com.example.dummyapp.order.service.OrderServiceApi;

@Component
public class CtorInterfaceInjectedOrderCommandHandler extends CommandHandler {

    private final OrderServiceApi orderService;

    @Autowired
    public CtorInterfaceInjectedOrderCommandHandler(OrderServiceApi orderService) {
        this.orderService = orderService;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Order";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetOrderCommand cmd -> handleGetOrder(cmd);
            default -> null;
        };
    }

    private Object handleGetOrder(GetOrderCommand cmd) {
        return orderService.getOrder(cmd.getOrderAggregateId(), cmd.getUnitOfWork());
    }
}
