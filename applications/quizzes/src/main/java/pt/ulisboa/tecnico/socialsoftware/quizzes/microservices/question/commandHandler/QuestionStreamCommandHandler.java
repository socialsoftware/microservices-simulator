package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.StreamCommandHandler;

import java.util.function.Consumer;

@Component
public class QuestionStreamCommandHandler extends StreamCommandHandler {


    private final QuestionCommandHandler questionCommandHandler;

    @Autowired
    public QuestionStreamCommandHandler(StreamBridge streamBridge, QuestionCommandHandler questionCommandHandler) {
        super(streamBridge);
        this.questionCommandHandler = questionCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        return questionCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<Command>> questionServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}

