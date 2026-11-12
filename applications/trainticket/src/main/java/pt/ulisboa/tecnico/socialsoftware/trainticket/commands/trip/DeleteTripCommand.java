package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class DeleteTripCommand extends Command {
    private Integer tripAggregateId;

    public DeleteTripCommand(UnitOfWork unitOfWork, String serviceName, Integer tripAggregateId) {
        super(unitOfWork, serviceName, tripAggregateId);
        this.tripAggregateId = tripAggregateId;
    }

    public Integer getTripAggregateId() {
        return tripAggregateId;
    }
}
