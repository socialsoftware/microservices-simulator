package pt.ulisboa.tecnico.socialsoftware.showcase.command.room;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomAmenityDto;

public class UpdateRoomAmenitieCommand extends Command {
    private final Integer roomId;
    private final Integer code;
    private final RoomAmenityDto amenitieDto;

    public UpdateRoomAmenitieCommand(UnitOfWork unitOfWork, String serviceName, Integer roomId, Integer code, RoomAmenityDto amenitieDto) {
        super(unitOfWork, serviceName, null);
        this.roomId = roomId;
        this.code = code;
        this.amenitieDto = amenitieDto;
    }

    public Integer getRoomId() { return roomId; }
    public Integer getCode() { return code; }
    public RoomAmenityDto getAmenitieDto() { return amenitieDto; }
}
