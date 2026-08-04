package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.commandHandler;

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
public class RouteStreamCommandHandler extends StreamCommandHandler {

    private final RouteCommandHandler routeCommandHandler;

    @Autowired
    public RouteStreamCommandHandler(StreamBridge streamBridge,
            RouteCommandHandler routeCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.routeCommandHandler = routeCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Route";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return routeCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> routeServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
