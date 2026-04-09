package com.example.dummyapp.order.commandHandler;

import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import com.example.dummyapp.order.commands.CancelOrderCommand;
import com.example.dummyapp.order.commands.GetOrderCommand;
import com.example.dummyapp.order.commands.PlaceOrderCommand;
import com.example.dummyapp.order.service.OrderService;

import java.util.logging.Logger;

@Component
public class OrderCommandHandler extends CommandHandler {

    private static final Logger logger = Logger.getLogger(OrderCommandHandler.class.getName());

    private final OrderService orderService;

    public OrderCommandHandler(OrderService orderService) {
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
            case PlaceOrderCommand cmd -> handlePlaceOrder(cmd);
            case CancelOrderCommand cmd -> handleCancelOrder(cmd);
            default -> null;
        };
    }

    private Object handleGetOrder(GetOrderCommand cmd) {
        logger.info("Getting order: " + cmd.getOrderAggregateId());
        return orderService.getOrder(cmd.getOrderAggregateId(), cmd.getUnitOfWork());
    }

    private Object handlePlaceOrder(PlaceOrderCommand cmd) {
        logger.info("Placing order");
        return orderService.placeOrder(cmd.getOrderDto(), cmd.getUnitOfWork());
    }

    private Object handleCancelOrder(CancelOrderCommand cmd) {
        logger.info("Cancelling order: " + cmd.getOrderAggregateId());
        orderService.cancelOrder(cmd.getOrderAggregateId(), cmd.getUnitOfWork());
        return null;
    }
}
