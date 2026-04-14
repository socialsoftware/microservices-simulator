package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.commandHandler;

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
public class BookingStreamCommandHandler extends StreamCommandHandler {

    private final BookingCommandHandler bookingCommandHandler;

    @Autowired
    public BookingStreamCommandHandler(StreamBridge streamBridge,
            BookingCommandHandler bookingCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.bookingCommandHandler = bookingCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Booking";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return bookingCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> bookingServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
