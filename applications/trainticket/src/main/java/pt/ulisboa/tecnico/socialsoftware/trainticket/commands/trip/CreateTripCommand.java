package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;

public class CreateTripCommand extends Command {
    private TripDto tripDto;

    public CreateTripCommand(UnitOfWork unitOfWork, String serviceName, TripDto tripDto) {
        super(unitOfWork, serviceName, null);
        this.tripDto = tripDto;
    }

    public TripDto getTripDto() {
        return tripDto;
    }
}
