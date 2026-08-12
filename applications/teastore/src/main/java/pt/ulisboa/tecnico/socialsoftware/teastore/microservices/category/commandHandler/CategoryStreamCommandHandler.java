package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.commandHandler;

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
public class CategoryStreamCommandHandler extends StreamCommandHandler {

    private final CategoryCommandHandler categoryCommandHandler;

    @Autowired
    public CategoryStreamCommandHandler(StreamBridge streamBridge,
            CategoryCommandHandler categoryCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.categoryCommandHandler = categoryCommandHandler;
    }

    @Override
    public String getAggregateTypeName() {
        return "Category";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return categoryCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> categoryServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
