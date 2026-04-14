package pt.ulisboa.tecnico.socialsoftware.showcase.command.room;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto;

public class CreateRoomCommand extends Command {
    private final CreateRoomRequestDto createRequest;

    public CreateRoomCommand(UnitOfWork unitOfWork, String serviceName, CreateRoomRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateRoomRequestDto getCreateRequest() { return createRequest; }
}
