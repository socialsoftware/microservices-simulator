package pt.ulisboa.tecnico.socialsoftware.trainticket.command.station;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.webapi.requestDtos.CreateStationRequestDto;

public class CreateStationCommand extends Command {
    private final CreateStationRequestDto createRequest;

    public CreateStationCommand(UnitOfWork unitOfWork, String serviceName, CreateStationRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateStationRequestDto getCreateRequest() { return createRequest; }
}
