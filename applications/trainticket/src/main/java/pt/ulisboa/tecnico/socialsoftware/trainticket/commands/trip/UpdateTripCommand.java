package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;

public class UpdateTripCommand extends Command {
    private Integer tripAggregateId;
    private TripDto tripDto;

    public UpdateTripCommand(UnitOfWork unitOfWork, String serviceName,
                             Integer tripAggregateId, TripDto tripDto) {
        super(unitOfWork, serviceName, tripAggregateId);
        this.tripAggregateId = tripAggregateId;
        this.tripDto = tripDto;
    }

    public Integer getTripAggregateId() {
        return tripAggregateId;
    }

    public TripDto getTripDto() {
        return tripDto;
    }
}
