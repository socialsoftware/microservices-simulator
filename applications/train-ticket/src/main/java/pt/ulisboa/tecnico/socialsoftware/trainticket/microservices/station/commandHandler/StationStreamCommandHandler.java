package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.commandHandler;

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
public class StationStreamCommandHandler extends StreamCommandHandler {

    private final StationCommandHandler stationCommandHandler;

    @Autowired
    public StationStreamCommandHandler(StreamBridge streamBridge,
            StationCommandHandler stationCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.stationCommandHandler = stationCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Station";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return stationCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> stationServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
