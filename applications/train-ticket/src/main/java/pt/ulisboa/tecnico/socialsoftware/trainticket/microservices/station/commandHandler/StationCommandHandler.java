package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.station.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service.StationService;

import java.util.logging.Logger;

@Component
public class StationCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(StationCommandHandler.class.getName());

    @Autowired
    private StationService stationService;

    @Override
    protected String getAggregateTypeName() {
        return "Station";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateStationCommand cmd -> handleCreateStation(cmd);
            case GetStationByIdCommand cmd -> handleGetStationById(cmd);
            case GetAllStationsCommand cmd -> handleGetAllStations(cmd);
            case UpdateStationCommand cmd -> handleUpdateStation(cmd);
            case DeleteStationCommand cmd -> handleDeleteStation(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateStation(CreateStationCommand cmd) {
        logger.info("handleCreateStation");
        try {
            return stationService.createStation(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetStationById(GetStationByIdCommand cmd) {
        logger.info("handleGetStationById");
        try {
            return stationService.getStationById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllStations(GetAllStationsCommand cmd) {
        logger.info("handleGetAllStations");
        try {
            return stationService.getAllStations(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateStation(UpdateStationCommand cmd) {
        logger.info("handleUpdateStation");
        try {
            return stationService.updateStation(cmd.getStationDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteStation(DeleteStationCommand cmd) {
        logger.info("handleDeleteStation");
        try {
            stationService.deleteStation(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
