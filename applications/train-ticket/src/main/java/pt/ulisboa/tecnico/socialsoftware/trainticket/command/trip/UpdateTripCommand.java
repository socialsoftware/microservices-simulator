package pt.ulisboa.tecnico.socialsoftware.trainticket.command.trip;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;

public class UpdateTripCommand extends Command {
    private final TripDto tripDto;

    public UpdateTripCommand(UnitOfWork unitOfWork, String serviceName, TripDto tripDto) {
        super(unitOfWork, serviceName, null);
        this.tripDto = tripDto;
    }

    public TripDto getTripDto() { return tripDto; }
}
