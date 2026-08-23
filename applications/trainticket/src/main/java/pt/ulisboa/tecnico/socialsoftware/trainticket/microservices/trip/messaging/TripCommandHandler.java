package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.GetTripByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.GetTripsCommand;
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
            case GetTripByIdCommand cmd -> handleGetTripById(cmd);
            case GetTripsCommand cmd -> handleGetTrips(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetTripById(GetTripByIdCommand command) {
        return tripService.getTripById(command.getTripAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetTrips(GetTripsCommand command) {
        return tripService.getTrips(command.getUnitOfWork());
    }
}
