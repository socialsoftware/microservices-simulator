package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.cart.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.service.CartService;

import java.util.logging.Logger;

@Component
public class CartCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(CartCommandHandler.class.getName());

    @Autowired
    private CartService cartService;

    @Override
    protected String getAggregateTypeName() {
        return "Cart";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateCartCommand cmd -> handleCreateCart(cmd);
            case GetCartByIdCommand cmd -> handleGetCartById(cmd);
            case GetAllCartsCommand cmd -> handleGetAllCarts(cmd);
            case UpdateCartCommand cmd -> handleUpdateCart(cmd);
            case DeleteCartCommand cmd -> handleDeleteCart(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateCart(CreateCartCommand cmd) {
        logger.info("handleCreateCart");
        try {
            return cartService.createCart(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetCartById(GetCartByIdCommand cmd) {
        logger.info("handleGetCartById");
        try {
            return cartService.getCartById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllCarts(GetAllCartsCommand cmd) {
        logger.info("handleGetAllCarts");
        try {
            return cartService.getAllCarts(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateCart(UpdateCartCommand cmd) {
        logger.info("handleUpdateCart");
        try {
            return cartService.updateCart(cmd.getCartDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteCart(DeleteCartCommand cmd) {
        logger.info("handleDeleteCart");
        try {
            cartService.deleteCart(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
