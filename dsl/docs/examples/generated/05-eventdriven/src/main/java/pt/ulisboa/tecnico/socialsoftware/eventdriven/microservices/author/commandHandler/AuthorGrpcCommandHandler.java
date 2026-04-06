package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class AuthorGrpcCommandHandler extends GrpcCommandHandler {

    private final AuthorCommandHandler authorCommandHandler;

    public AuthorGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            AuthorCommandHandler authorCommandHandler) {
        super(mapperProvider);
        this.authorCommandHandler = authorCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Author";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return authorCommandHandler.handleDomainCommand(command);
    }
}
