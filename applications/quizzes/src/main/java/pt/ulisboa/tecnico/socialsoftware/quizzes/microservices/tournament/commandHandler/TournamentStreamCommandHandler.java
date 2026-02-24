package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.StreamCommandHandler;

import java.util.function.Consumer;

@Component
@Profile("stream")
public class TournamentStreamCommandHandler extends StreamCommandHandler {

    private final TournamentCommandHandler tournamentCommandHandler;

    @Autowired
    public TournamentStreamCommandHandler(StreamBridge streamBridge,
            TournamentCommandHandler tournamentCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
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

    @Bean
    public Consumer<Message<?>> tournamentServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
