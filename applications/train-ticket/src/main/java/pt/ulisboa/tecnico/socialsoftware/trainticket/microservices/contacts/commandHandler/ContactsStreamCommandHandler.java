package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.commandHandler;

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
public class ContactsStreamCommandHandler extends StreamCommandHandler {

    private final ContactsCommandHandler contactsCommandHandler;

    @Autowired
    public ContactsStreamCommandHandler(StreamBridge streamBridge,
            ContactsCommandHandler contactsCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.contactsCommandHandler = contactsCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Contacts";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return contactsCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> contactsServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
