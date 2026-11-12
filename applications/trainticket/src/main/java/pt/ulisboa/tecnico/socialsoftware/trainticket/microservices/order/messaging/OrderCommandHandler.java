package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.CancelOrderCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.CollectTicketCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.DeleteOrderCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.GetLeftTicketCountCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.GetOrderByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.GetOrdersByAccountCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.GetOrdersCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.PayOrderCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.PreserveTicketCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.UseTicketCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.service.OrderService;

import java.util.logging.Logger;

@Component
public class OrderCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(OrderCommandHandler.class.getName());

    @Autowired
    private OrderService orderService;

    @Override
    public String getAggregateTypeName() {
        return "Order";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetOrderByIdCommand cmd -> handleGetOrderById(cmd);
            case GetOrdersCommand cmd -> handleGetOrders(cmd);
            case GetOrdersByAccountCommand cmd -> handleGetOrdersByAccount(cmd);
            case GetLeftTicketCountCommand cmd -> handleGetLeftTicketCount(cmd);
            case PreserveTicketCommand cmd -> handlePreserveTicket(cmd);
            case PayOrderCommand cmd -> handlePayOrder(cmd);
            case CollectTicketCommand cmd -> handleCollectTicket(cmd);
            case UseTicketCommand cmd -> handleUseTicket(cmd);
            case CancelOrderCommand cmd -> handleCancelOrder(cmd);
            case DeleteOrderCommand cmd -> handleDeleteOrder(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetOrderById(GetOrderByIdCommand command) {
        return orderService.getOrderById(command.getOrderAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetOrders(GetOrdersCommand command) {
        return orderService.getOrders(command.getUnitOfWork());
    }

    private Object handleGetOrdersByAccount(GetOrdersByAccountCommand command) {
        return orderService.getOrdersByAccount(command.getUserAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetLeftTicketCount(GetLeftTicketCountCommand command) {
        return orderService.getLeftTicketCount(command.getTripAggregateId(), command.getTravelDate(),
                command.getSeatClass(), command.getCapacity(), command.getUnitOfWork());
    }

    private Object handlePreserveTicket(PreserveTicketCommand command) {
        return orderService.preserveTicket(command.getOrderDto(), command.getContactsDto(),
                command.getTripDto(), command.getRouteDto(), command.getPriceConfigDto(),
                command.getCapacity(), command.getUnitOfWork());
    }

    private Object handlePayOrder(PayOrderCommand command) {
        orderService.payOrder(command.getOrderAggregateId(), command.getUnitOfWork());
        return null;
    }

    private Object handleCollectTicket(CollectTicketCommand command) {
        orderService.collectTicket(command.getOrderAggregateId(), command.getUnitOfWork());
        return null;
    }

    private Object handleUseTicket(UseTicketCommand command) {
        orderService.useTicket(command.getOrderAggregateId(), command.getUnitOfWork());
        return null;
    }

    private Object handleCancelOrder(CancelOrderCommand command) {
        orderService.cancelOrder(command.getOrderAggregateId(), command.getUnitOfWork());
        return null;
    }

    private Object handleDeleteOrder(DeleteOrderCommand command) {
        orderService.deleteOrder(command.getOrderAggregateId(), command.getUnitOfWork());
        return null;
    }
}
