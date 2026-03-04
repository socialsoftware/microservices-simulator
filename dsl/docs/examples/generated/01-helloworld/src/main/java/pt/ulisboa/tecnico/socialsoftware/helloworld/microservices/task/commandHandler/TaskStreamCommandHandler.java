package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.StreamCommandHandler;

import java.util.function.Consumer;

@Component
@Profile("stream")
public class TaskStreamCommandHandler extends StreamCommandHandler {

    private final TaskCommandHandler taskCommandHandler;

    @Autowired
    public TaskStreamCommandHandler(StreamBridge streamBridge,
            TaskCommandHandler taskCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.taskCommandHandler = taskCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Task";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return taskCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> taskServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
