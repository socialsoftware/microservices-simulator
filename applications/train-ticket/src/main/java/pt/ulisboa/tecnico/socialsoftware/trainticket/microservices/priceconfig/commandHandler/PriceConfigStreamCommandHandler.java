package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.commandHandler;

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
public class PriceConfigStreamCommandHandler extends StreamCommandHandler {

    private final PriceConfigCommandHandler priceconfigCommandHandler;

    @Autowired
    public PriceConfigStreamCommandHandler(StreamBridge streamBridge,
            PriceConfigCommandHandler priceconfigCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.priceconfigCommandHandler = priceconfigCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "PriceConfig";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return priceconfigCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> priceconfigServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
