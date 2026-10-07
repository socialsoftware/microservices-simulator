package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.CreateStationCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service.StationService;

import java.util.logging.Logger;

@Component
public class StationCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(StationCommandHandler.class.getName());

    @Autowired
    private StationService stationService;

    @Override
    public String getAggregateTypeName() {
        return "Station";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateStationCommand cmd -> handleCreateStation(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateStation(CreateStationCommand command) {
        return stationService.createStation(command.getStationDto(), command.getUnitOfWork());
    }
}
