package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.commandHandler;

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
public class ShippingStreamCommandHandler extends StreamCommandHandler {

    private final ShippingCommandHandler shippingCommandHandler;

    @Autowired
    public ShippingStreamCommandHandler(StreamBridge streamBridge,
            ShippingCommandHandler shippingCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.shippingCommandHandler = shippingCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Shipping";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return shippingCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> shippingServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
