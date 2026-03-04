package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.commandHandler;

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
public class BookStreamCommandHandler extends StreamCommandHandler {

    private final BookCommandHandler bookCommandHandler;

    @Autowired
    public BookStreamCommandHandler(StreamBridge streamBridge,
            BookCommandHandler bookCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.bookCommandHandler = bookCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Book";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return bookCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> bookServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
