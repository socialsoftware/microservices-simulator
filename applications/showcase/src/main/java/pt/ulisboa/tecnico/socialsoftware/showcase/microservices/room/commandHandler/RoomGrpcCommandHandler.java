package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class RoomGrpcCommandHandler extends GrpcCommandHandler {

    private final RoomCommandHandler roomCommandHandler;

    public RoomGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            RoomCommandHandler roomCommandHandler) {
        super(mapperProvider);
        this.roomCommandHandler = roomCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Room";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return roomCommandHandler.handleDomainCommand(command);
    }
}
