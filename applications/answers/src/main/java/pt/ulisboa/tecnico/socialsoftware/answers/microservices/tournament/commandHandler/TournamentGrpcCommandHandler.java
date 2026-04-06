package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class TournamentGrpcCommandHandler extends GrpcCommandHandler {

    private final TournamentCommandHandler tournamentCommandHandler;

    public TournamentGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            TournamentCommandHandler tournamentCommandHandler) {
        super(mapperProvider);
        this.tournamentCommandHandler = tournamentCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Tournament";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return tournamentCommandHandler.handleDomainCommand(command);
    }
}
