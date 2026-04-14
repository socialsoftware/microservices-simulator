package pt.ulisboa.tecnico.socialsoftware.showcase.command.room;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;

public class UpdateRoomCommand extends Command {
    private final RoomDto roomDto;

    public UpdateRoomCommand(UnitOfWork unitOfWork, String serviceName, RoomDto roomDto) {
        super(unitOfWork, serviceName, null);
        this.roomDto = roomDto;
    }

    public RoomDto getRoomDto() { return roomDto; }
}
