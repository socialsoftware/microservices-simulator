package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.shipping.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.service.ShippingService;

import java.util.logging.Logger;

@Component
public class ShippingCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(ShippingCommandHandler.class.getName());

    @Autowired
    private ShippingService shippingService;

    @Override
    protected String getAggregateTypeName() {
        return "Shipping";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateShippingCommand cmd -> handleCreateShipping(cmd);
            case GetShippingByIdCommand cmd -> handleGetShippingById(cmd);
            case GetAllShippingsCommand cmd -> handleGetAllShippings(cmd);
            case UpdateShippingCommand cmd -> handleUpdateShipping(cmd);
            case DeleteShippingCommand cmd -> handleDeleteShipping(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateShipping(CreateShippingCommand cmd) {
        logger.info("handleCreateShipping");
        try {
            return shippingService.createShipping(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetShippingById(GetShippingByIdCommand cmd) {
        logger.info("handleGetShippingById");
        try {
            return shippingService.getShippingById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllShippings(GetAllShippingsCommand cmd) {
        logger.info("handleGetAllShippings");
        try {
            return shippingService.getAllShippings(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateShipping(UpdateShippingCommand cmd) {
        logger.info("handleUpdateShipping");
        try {
            return shippingService.updateShipping(cmd.getShippingDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteShipping(DeleteShippingCommand cmd) {
        logger.info("handleDeleteShipping");
        try {
            shippingService.deleteShipping(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
