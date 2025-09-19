package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.StreamCommandHandler;

import java.util.function.Consumer;

@Component
public class TopicStreamCommandHandler extends StreamCommandHandler {


    private final TopicCommandHandler topicCommandHandler;

    @Autowired
    public TopicStreamCommandHandler(StreamBridge streamBridge, TopicCommandHandler topicCommandHandler) {
        super(streamBridge);
        this.topicCommandHandler = topicCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        return topicCommandHandler.handle(command);
    }

    // Define the consumer function that Spring Cloud Stream will use
    @Bean
    public Consumer<Message<Command>> topicServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}

