package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.commandHandler;

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
public class ProductStreamCommandHandler extends StreamCommandHandler {

    private final ProductCommandHandler productCommandHandler;

    @Autowired
    public ProductStreamCommandHandler(StreamBridge streamBridge,
            ProductCommandHandler productCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.productCommandHandler = productCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Product";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return productCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> productServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
