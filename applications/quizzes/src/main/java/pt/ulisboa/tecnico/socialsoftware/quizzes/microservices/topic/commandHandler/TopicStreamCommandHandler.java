package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.commandHandler;

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
public class TopicStreamCommandHandler extends StreamCommandHandler {

    private final TopicCommandHandler topicCommandHandler;

    @Autowired
    public TopicStreamCommandHandler(StreamBridge streamBridge,
            TopicCommandHandler topicCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.topicCommandHandler = topicCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Topic";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return topicCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> topicServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
