package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler;

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
public class UserStreamCommandHandler extends StreamCommandHandler {

    private final UserCommandHandler userCommandHandler;

    @Autowired
    public UserStreamCommandHandler(StreamBridge streamBridge,
                                    UserCommandHandler userCommandHandler,
                                    MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.userCommandHandler = userCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        return userCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<?>> userServiceCommandChannel() {
        System.out.println("Registering user service command channel");
        return this::handleCommandMessage;
    }
}

