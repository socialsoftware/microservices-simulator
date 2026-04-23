package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;

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
            case CreateOrderCommand cmd -> handleCreateOrder(cmd);
            case GetOrderByIdCommand cmd -> handleGetOrderById(cmd);
            case GetAllOrdersCommand cmd -> handleGetAllOrders(cmd);
            case UpdateOrderCommand cmd -> handleUpdateOrder(cmd);
            case DeleteOrderCommand cmd -> handleDeleteOrder(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateOrder(CreateOrderCommand cmd) {
        logger.info("handleCreateOrder");
        try {
            return orderService.createOrder(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetOrderById(GetOrderByIdCommand cmd) {
        logger.info("handleGetOrderById");
        try {
            return orderService.getOrderById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllOrders(GetAllOrdersCommand cmd) {
        logger.info("handleGetAllOrders");
        try {
            return orderService.getAllOrders(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateOrder(UpdateOrderCommand cmd) {
        logger.info("handleUpdateOrder");
        try {
            return orderService.updateOrder(cmd.getOrderDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteOrder(DeleteOrderCommand cmd) {
        logger.info("handleDeleteOrder");
        try {
            orderService.deleteOrder(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
