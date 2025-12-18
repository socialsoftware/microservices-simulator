package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.commandHandler;

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
public class AnswerStreamCommandHandler extends StreamCommandHandler {

    private final AnswerCommandHandler answerCommandHandler;

    @Autowired
    public AnswerStreamCommandHandler(StreamBridge streamBridge,
            AnswerCommandHandler answerCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.answerCommandHandler = answerCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "QuizAnswer";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return answerCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> answerServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
