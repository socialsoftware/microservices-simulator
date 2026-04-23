package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.commandHandler;

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
    public String getAggregateTypeName() {
        return "Order";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return orderCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> orderServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
