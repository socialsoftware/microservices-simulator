package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class PostGrpcCommandHandler extends GrpcCommandHandler {

    private final PostCommandHandler postCommandHandler;

    public PostGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            PostCommandHandler postCommandHandler) {
        super(mapperProvider);
        this.postCommandHandler = postCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Post";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return postCommandHandler.handleDomainCommand(command);
    }
}
