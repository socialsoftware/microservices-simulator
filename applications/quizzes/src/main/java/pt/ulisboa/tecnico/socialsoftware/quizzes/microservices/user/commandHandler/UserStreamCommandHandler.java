package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.StreamCommandHandler;

import java.util.function.Consumer;

@Component
public class UserStreamCommandHandler extends StreamCommandHandler {


    private final UserCommandHandler userCommandHandler;

    @Autowired
    public UserStreamCommandHandler(StreamBridge streamBridge, UserCommandHandler userCommandHandler) {
        super(streamBridge);
        this.userCommandHandler = userCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        return userCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<Command>> userServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}

