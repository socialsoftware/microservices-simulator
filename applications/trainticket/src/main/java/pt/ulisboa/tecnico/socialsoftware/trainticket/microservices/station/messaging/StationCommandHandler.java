package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.CreateStationCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.DeleteStationCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.GetStationByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.GetStationsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.UpdateStationCommand;
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
            case GetStationByIdCommand cmd -> handleGetStationById(cmd);
            case GetStationsCommand cmd -> handleGetStations(cmd);
            case CreateStationCommand cmd -> handleCreateStation(cmd);
            case UpdateStationCommand cmd -> handleUpdateStation(cmd);
            case DeleteStationCommand cmd -> handleDeleteStation(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetStationById(GetStationByIdCommand command) {
        return stationService.getStationById(command.getStationAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetStations(GetStationsCommand command) {
        return stationService.getStations(command.getUnitOfWork());
    }

    private Object handleCreateStation(CreateStationCommand command) {
        return stationService.createStation(command.getStationDto(), command.getUnitOfWork());
    }

    private Object handleUpdateStation(UpdateStationCommand command) {
        stationService.updateStation(command.getStationAggregateId(), command.getStationDto(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleDeleteStation(DeleteStationCommand command) {
        stationService.deleteStation(command.getStationAggregateId(), command.getUnitOfWork());
        return null;
    }
}
