package pt.ulisboa.tecnico.socialsoftware.showcase.command.room;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomAmenityDto;
import java.util.List;

public class AddRoomAmenitiesCommand extends Command {
    private final Integer roomId;
    private final List<RoomAmenityDto> amenitieDtos;

    public AddRoomAmenitiesCommand(UnitOfWork unitOfWork, String serviceName, Integer roomId, List<RoomAmenityDto> amenitieDtos) {
        super(unitOfWork, serviceName, null);
        this.roomId = roomId;
        this.amenitieDtos = amenitieDtos;
    }

    public Integer getRoomId() { return roomId; }
    public List<RoomAmenityDto> getAmenitieDtos() { return amenitieDtos; }
}
