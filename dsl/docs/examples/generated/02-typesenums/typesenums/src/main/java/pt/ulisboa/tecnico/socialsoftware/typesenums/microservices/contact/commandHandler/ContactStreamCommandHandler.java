package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.commandHandler;

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
public class ContactStreamCommandHandler extends StreamCommandHandler {

    private final ContactCommandHandler contactCommandHandler;

    @Autowired
    public ContactStreamCommandHandler(StreamBridge streamBridge,
            ContactCommandHandler contactCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.contactCommandHandler = contactCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Contact";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return contactCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> contactServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
