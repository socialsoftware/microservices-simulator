package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.CreateTripCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.DeleteTripCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.GetTripByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.GetTripsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.UpdateTripCommand;
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
            case CreateTripCommand cmd -> handleCreateTrip(cmd);
            case UpdateTripCommand cmd -> handleUpdateTrip(cmd);
            case DeleteTripCommand cmd -> handleDeleteTrip(cmd);
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

    private Object handleCreateTrip(CreateTripCommand command) {
        return tripService.createTrip(command.getTripDto(), command.getUnitOfWork());
    }

    private Object handleUpdateTrip(UpdateTripCommand command) {
        tripService.updateTrip(command.getTripAggregateId(), command.getTripDto(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleDeleteTrip(DeleteTripCommand command) {
        tripService.deleteTrip(command.getTripAggregateId(), command.getUnitOfWork());
        return null;
    }
}
