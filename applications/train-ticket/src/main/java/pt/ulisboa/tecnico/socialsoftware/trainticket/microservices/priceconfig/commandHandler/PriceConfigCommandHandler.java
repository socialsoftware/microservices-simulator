package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.priceconfig.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.service.PriceConfigService;

import java.util.logging.Logger;

@Component
public class PriceConfigCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(PriceConfigCommandHandler.class.getName());

    @Autowired
    private PriceConfigService priceconfigService;

    @Override
    protected String getAggregateTypeName() {
        return "PriceConfig";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreatePriceConfigCommand cmd -> handleCreatePriceConfig(cmd);
            case GetPriceConfigByIdCommand cmd -> handleGetPriceConfigById(cmd);
            case GetAllPriceConfigsCommand cmd -> handleGetAllPriceConfigs(cmd);
            case UpdatePriceConfigCommand cmd -> handleUpdatePriceConfig(cmd);
            case DeletePriceConfigCommand cmd -> handleDeletePriceConfig(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreatePriceConfig(CreatePriceConfigCommand cmd) {
        logger.info("handleCreatePriceConfig");
        try {
            return priceconfigService.createPriceConfig(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetPriceConfigById(GetPriceConfigByIdCommand cmd) {
        logger.info("handleGetPriceConfigById");
        try {
            return priceconfigService.getPriceConfigById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllPriceConfigs(GetAllPriceConfigsCommand cmd) {
        logger.info("handleGetAllPriceConfigs");
        try {
            return priceconfigService.getAllPriceConfigs(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdatePriceConfig(UpdatePriceConfigCommand cmd) {
        logger.info("handleUpdatePriceConfig");
        try {
            return priceconfigService.updatePriceConfig(cmd.getPriceConfigDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeletePriceConfig(DeletePriceConfigCommand cmd) {
        logger.info("handleDeletePriceConfig");
        try {
            priceconfigService.deletePriceConfig(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
