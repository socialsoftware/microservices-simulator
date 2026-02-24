package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.commandHandler;

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
public class OrderStreamCommandHandler extends StreamCommandHandler {

    private final OrderCommandHandler orderCommandHandler;

    @Autowired
    public OrderStreamCommandHandler(StreamBridge streamBridge,
            OrderCommandHandler orderCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.orderCommandHandler = orderCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Order";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return orderCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> orderServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
