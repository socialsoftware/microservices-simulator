package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.commandHandler;

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
public class QuizStreamCommandHandler extends StreamCommandHandler {


    private final QuizCommandHandler quizCommandHandler;

    @Autowired
    public QuizStreamCommandHandler(StreamBridge streamBridge,
                                    QuizCommandHandler quizCommandHandler,
                                    MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.quizCommandHandler = quizCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        return quizCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<?>> quizServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}

