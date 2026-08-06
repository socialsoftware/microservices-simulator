package pt.ulisboa.tecnico.socialsoftware.trainticket.command.trip;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.webapi.requestDtos.CreateTripRequestDto;

public class CreateTripCommand extends Command {
    private final CreateTripRequestDto createRequest;

    public CreateTripCommand(UnitOfWork unitOfWork, String serviceName, CreateTripRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateTripRequestDto getCreateRequest() { return createRequest; }
}
