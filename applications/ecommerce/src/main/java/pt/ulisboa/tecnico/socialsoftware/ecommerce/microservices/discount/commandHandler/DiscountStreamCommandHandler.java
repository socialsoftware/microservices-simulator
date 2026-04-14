package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.commandHandler;

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
public class DiscountStreamCommandHandler extends StreamCommandHandler {

    private final DiscountCommandHandler discountCommandHandler;

    @Autowired
    public DiscountStreamCommandHandler(StreamBridge streamBridge,
            DiscountCommandHandler discountCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.discountCommandHandler = discountCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Discount";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return discountCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> discountServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
