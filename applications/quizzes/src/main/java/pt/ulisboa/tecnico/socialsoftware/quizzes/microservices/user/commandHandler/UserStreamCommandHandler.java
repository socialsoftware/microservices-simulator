package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.StreamCommandHandler;

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
    protected String getAggregateTypeName() {
        return "User";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return userCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> userServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
