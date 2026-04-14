package pt.ulisboa.tecnico.socialsoftware.showcase.command.room;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetRoomAmenitieCommand extends Command {
    private final Integer roomId;
    private final Integer code;

    public GetRoomAmenitieCommand(UnitOfWork unitOfWork, String serviceName, Integer roomId, Integer code) {
        super(unitOfWork, serviceName, null);
        this.roomId = roomId;
        this.code = code;
    }

    public Integer getRoomId() { return roomId; }
    public Integer getCode() { return code; }
}
