package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

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
public class VersionStreamCommandHandler extends StreamCommandHandler {

    private final VersionCommandHandler versionCommandHandler;

    @Autowired
    public VersionStreamCommandHandler(StreamBridge streamBridge,
            VersionCommandHandler versionCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.versionCommandHandler = versionCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Version";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        // Delegate to the standalone VersionCommandHandler
        return versionCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<?>> versionCommandChannel() {
        return this::handleCommandMessage;
    }
}
