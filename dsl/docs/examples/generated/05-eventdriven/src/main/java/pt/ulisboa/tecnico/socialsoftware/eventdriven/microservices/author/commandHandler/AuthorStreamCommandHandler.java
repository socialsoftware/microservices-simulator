package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.StreamCommandHandler;

import java.util.function.Consumer;

@Component
@Profile("stream")
public class AuthorStreamCommandHandler extends StreamCommandHandler {

    private final AuthorCommandHandler authorCommandHandler;

    @Autowired
    public AuthorStreamCommandHandler(StreamBridge streamBridge,
            AuthorCommandHandler authorCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
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

    @Bean
    public Consumer<Message<?>> authorServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
