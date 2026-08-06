package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.trip.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.service.TripService;

import java.util.logging.Logger;

@Component
public class TripCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TripCommandHandler.class.getName());

    @Autowired
    private TripService tripService;

    @Override
    public String getAggregateTypeName() {
        return "Trip";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateTripCommand cmd -> handleCreateTrip(cmd);
            case GetTripByIdCommand cmd -> handleGetTripById(cmd);
            case GetAllTripsCommand cmd -> handleGetAllTrips(cmd);
            case UpdateTripCommand cmd -> handleUpdateTrip(cmd);
            case DeleteTripCommand cmd -> handleDeleteTrip(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateTrip(CreateTripCommand cmd) {
        logger.info("handleCreateTrip");
        try {
            return tripService.createTrip(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetTripById(GetTripByIdCommand cmd) {
        logger.info("handleGetTripById");
        try {
            return tripService.getTripById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllTrips(GetAllTripsCommand cmd) {
        logger.info("handleGetAllTrips");
        try {
            return tripService.getAllTrips(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateTrip(UpdateTripCommand cmd) {
        logger.info("handleUpdateTrip");
        try {
            return tripService.updateTrip(cmd.getTripDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteTrip(DeleteTripCommand cmd) {
        logger.info("handleDeleteTrip");
        try {
            tripService.deleteTrip(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
