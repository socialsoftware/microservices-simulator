package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.commandHandler;

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
public class CartStreamCommandHandler extends StreamCommandHandler {

    private final CartCommandHandler cartCommandHandler;

    @Autowired
    public CartStreamCommandHandler(StreamBridge streamBridge,
            CartCommandHandler cartCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.cartCommandHandler = cartCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Cart";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return cartCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> cartServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
