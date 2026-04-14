package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.wishlistitem.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.service.WishlistItemService;

import java.util.logging.Logger;

@Component
public class WishlistItemCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(WishlistItemCommandHandler.class.getName());

    @Autowired
    private WishlistItemService wishlistitemService;

    @Override
    protected String getAggregateTypeName() {
        return "WishlistItem";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateWishlistItemCommand cmd -> handleCreateWishlistItem(cmd);
            case GetWishlistItemByIdCommand cmd -> handleGetWishlistItemById(cmd);
            case GetAllWishlistItemsCommand cmd -> handleGetAllWishlistItems(cmd);
            case UpdateWishlistItemCommand cmd -> handleUpdateWishlistItem(cmd);
            case DeleteWishlistItemCommand cmd -> handleDeleteWishlistItem(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateWishlistItem(CreateWishlistItemCommand cmd) {
        logger.info("handleCreateWishlistItem");
        try {
            return wishlistitemService.createWishlistItem(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetWishlistItemById(GetWishlistItemByIdCommand cmd) {
        logger.info("handleGetWishlistItemById");
        try {
            return wishlistitemService.getWishlistItemById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllWishlistItems(GetAllWishlistItemsCommand cmd) {
        logger.info("handleGetAllWishlistItems");
        try {
            return wishlistitemService.getAllWishlistItems(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateWishlistItem(UpdateWishlistItemCommand cmd) {
        logger.info("handleUpdateWishlistItem");
        try {
            return wishlistitemService.updateWishlistItem(cmd.getWishlistItemDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteWishlistItem(DeleteWishlistItemCommand cmd) {
        logger.info("handleDeleteWishlistItem");
        try {
            wishlistitemService.deleteWishlistItem(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
