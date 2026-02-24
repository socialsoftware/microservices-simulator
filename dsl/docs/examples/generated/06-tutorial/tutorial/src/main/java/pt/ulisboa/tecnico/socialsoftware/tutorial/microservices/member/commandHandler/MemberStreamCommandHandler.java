package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.commandHandler;

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
public class MemberStreamCommandHandler extends StreamCommandHandler {

    private final MemberCommandHandler memberCommandHandler;

    @Autowired
    public MemberStreamCommandHandler(StreamBridge streamBridge,
            MemberCommandHandler memberCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.memberCommandHandler = memberCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Member";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return memberCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> memberServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
