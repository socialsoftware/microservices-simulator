package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.CreatePriceConfigCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.DeletePriceConfigCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.GetPriceConfigByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.GetPriceConfigByRouteAndTrainTypeCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.GetPriceConfigsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.UpdatePriceConfigCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.service.PriceConfigService;

import java.util.logging.Logger;

@Component
public class PriceConfigCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(PriceConfigCommandHandler.class.getName());

    @Autowired
    private PriceConfigService priceConfigService;

    @Override
    public String getAggregateTypeName() {
        return "PriceConfig";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetPriceConfigByIdCommand cmd -> handleGetPriceConfigById(cmd);
            case GetPriceConfigsCommand cmd -> handleGetPriceConfigs(cmd);
            case GetPriceConfigByRouteAndTrainTypeCommand cmd -> handleGetPriceConfigByRouteAndTrainType(cmd);
            case CreatePriceConfigCommand cmd -> handleCreatePriceConfig(cmd);
            case UpdatePriceConfigCommand cmd -> {
                handleUpdatePriceConfig(cmd);
                yield null;
            }
            case DeletePriceConfigCommand cmd -> {
                handleDeletePriceConfig(cmd);
                yield null;
            }
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetPriceConfigById(GetPriceConfigByIdCommand command) {
        return priceConfigService.getPriceConfigById(command.getPriceConfigAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetPriceConfigs(GetPriceConfigsCommand command) {
        return priceConfigService.getPriceConfigs(command.getUnitOfWork());
    }

    private Object handleGetPriceConfigByRouteAndTrainType(GetPriceConfigByRouteAndTrainTypeCommand command) {
        return priceConfigService.getPriceConfigByRouteAndTrainType(
                command.getRouteAggregateId(), command.getTrainTypeAggregateId(), command.getUnitOfWork());
    }

    private Object handleCreatePriceConfig(CreatePriceConfigCommand command) {
        return priceConfigService.createPriceConfig(command.getPriceConfigDto(), command.getUnitOfWork());
    }

    private void handleUpdatePriceConfig(UpdatePriceConfigCommand command) {
        priceConfigService.updatePriceConfig(command.getPriceConfigAggregateId(),
                command.getPriceConfigDto(), command.getUnitOfWork());
    }

    private void handleDeletePriceConfig(DeletePriceConfigCommand command) {
        priceConfigService.deletePriceConfig(command.getPriceConfigAggregateId(), command.getUnitOfWork());
    }
}
