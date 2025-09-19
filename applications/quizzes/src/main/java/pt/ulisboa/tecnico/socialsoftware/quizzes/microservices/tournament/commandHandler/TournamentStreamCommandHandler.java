package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.StreamCommandHandler;

import java.util.function.Consumer;

@Component
public class TournamentStreamCommandHandler extends StreamCommandHandler {

    private final TournamentCommandHandler tournamentCommandHandler;

    @Autowired
    public TournamentStreamCommandHandler(StreamBridge streamBridge, TournamentCommandHandler tournamentCommandHandler) {
        super(streamBridge);
        this.tournamentCommandHandler = tournamentCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        return tournamentCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<Command>> tournamentServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}

