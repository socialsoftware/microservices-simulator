package com.example.dummyapp.order.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import com.example.dummyapp.order.commands.CancelOrderCommand;
import com.example.dummyapp.order.service.OrderServiceApi;

@Component
public class InterfaceInjectedOrderCommandHandler extends CommandHandler {

    @Autowired
    private OrderServiceApi orderService;

    @Override
    protected String getAggregateTypeName() {
        return "Order";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CancelOrderCommand cmd -> handleCancelOrder(cmd);
            default -> null;
        };
    }

    private Object handleCancelOrder(CancelOrderCommand cmd) {
        orderService.cancelOrder(cmd.getOrderAggregateId(), cmd.getUnitOfWork());
        return null;
    }
}
