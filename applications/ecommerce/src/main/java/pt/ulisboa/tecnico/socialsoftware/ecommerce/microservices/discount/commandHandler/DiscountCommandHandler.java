package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.discount.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.service.DiscountService;

import java.util.logging.Logger;

@Component
public class DiscountCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(DiscountCommandHandler.class.getName());

    @Autowired
    private DiscountService discountService;

    @Override
    protected String getAggregateTypeName() {
        return "Discount";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateDiscountCommand cmd -> handleCreateDiscount(cmd);
            case GetDiscountByIdCommand cmd -> handleGetDiscountById(cmd);
            case GetAllDiscountsCommand cmd -> handleGetAllDiscounts(cmd);
            case UpdateDiscountCommand cmd -> handleUpdateDiscount(cmd);
            case DeleteDiscountCommand cmd -> handleDeleteDiscount(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateDiscount(CreateDiscountCommand cmd) {
        logger.info("handleCreateDiscount");
        try {
            return discountService.createDiscount(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetDiscountById(GetDiscountByIdCommand cmd) {
        logger.info("handleGetDiscountById");
        try {
            return discountService.getDiscountById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllDiscounts(GetAllDiscountsCommand cmd) {
        logger.info("handleGetAllDiscounts");
        try {
            return discountService.getAllDiscounts(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateDiscount(UpdateDiscountCommand cmd) {
        logger.info("handleUpdateDiscount");
        try {
            return discountService.updateDiscount(cmd.getDiscountDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteDiscount(DeleteDiscountCommand cmd) {
        logger.info("handleDeleteDiscount");
        try {
            discountService.deleteDiscount(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
