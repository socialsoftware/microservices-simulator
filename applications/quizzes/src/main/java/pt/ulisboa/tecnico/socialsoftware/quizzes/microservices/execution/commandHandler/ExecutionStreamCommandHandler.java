package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler;

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
public class ExecutionStreamCommandHandler extends StreamCommandHandler {

    private final ExecutionCommandHandler executionCommandHandler;

    @Autowired
    public ExecutionStreamCommandHandler(StreamBridge streamBridge,
                                         ExecutionCommandHandler executionCommandHandler,
                                         MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.executionCommandHandler = executionCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Execution";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return executionCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> executionServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
