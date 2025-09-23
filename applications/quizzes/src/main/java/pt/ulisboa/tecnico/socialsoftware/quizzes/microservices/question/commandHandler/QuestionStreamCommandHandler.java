package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.commandHandler;

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
public class QuestionStreamCommandHandler extends StreamCommandHandler {

    private final QuestionCommandHandler questionCommandHandler;

    @Autowired
    public QuestionStreamCommandHandler(StreamBridge streamBridge,
                                        QuestionCommandHandler questionCommandHandler,
                                        MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.questionCommandHandler = questionCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        return questionCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<?>> questionServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}

