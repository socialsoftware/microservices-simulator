package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.commandHandler;

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
public class TrainStreamCommandHandler extends StreamCommandHandler {

    private final TrainCommandHandler trainCommandHandler;

    @Autowired
    public TrainStreamCommandHandler(StreamBridge streamBridge,
            TrainCommandHandler trainCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.trainCommandHandler = trainCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Train";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return trainCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> trainServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
