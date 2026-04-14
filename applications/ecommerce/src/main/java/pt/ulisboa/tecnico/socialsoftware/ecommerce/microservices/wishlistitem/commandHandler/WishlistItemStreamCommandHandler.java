package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.commandHandler;

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
public class WishlistItemStreamCommandHandler extends StreamCommandHandler {

    private final WishlistItemCommandHandler wishlistitemCommandHandler;

    @Autowired
    public WishlistItemStreamCommandHandler(StreamBridge streamBridge,
            WishlistItemCommandHandler wishlistitemCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.wishlistitemCommandHandler = wishlistitemCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "WishlistItem";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return wishlistitemCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> wishlistitemServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
