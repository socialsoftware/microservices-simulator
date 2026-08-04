package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.commandHandler;

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
public class TripStreamCommandHandler extends StreamCommandHandler {

    private final TripCommandHandler tripCommandHandler;

    @Autowired
    public TripStreamCommandHandler(StreamBridge streamBridge,
            TripCommandHandler tripCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.tripCommandHandler = tripCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Trip";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return tripCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> tripServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
