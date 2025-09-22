package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.StreamCommandHandler;

import java.util.function.Consumer;

@Component
public class AnswerStreamCommandHandler extends StreamCommandHandler {

    private final AnswerCommandHandler answerCommandHandler;

    @Autowired
    public AnswerStreamCommandHandler(StreamBridge streamBridge, AnswerCommandHandler answerCommandHandler) {
        super(streamBridge);
        this.answerCommandHandler = answerCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        return answerCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<Command>> answerServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}

