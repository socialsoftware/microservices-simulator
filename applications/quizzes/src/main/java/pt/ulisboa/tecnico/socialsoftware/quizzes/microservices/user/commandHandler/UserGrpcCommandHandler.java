package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;

@Component
@Profile("grpc")
public class UserGrpcCommandHandler extends GrpcCommandHandler {

    private final UserCommandHandler userCommandHandler;

    public UserGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            UserCommandHandler userCommandHandler) {
        super(mapperProvider);
        this.userCommandHandler = userCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "User";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return userCommandHandler.handleDomainCommand(command);
    }
}
