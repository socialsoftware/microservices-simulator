package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.commandHandler;

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
public class EnrollmentStreamCommandHandler extends StreamCommandHandler {

    private final EnrollmentCommandHandler enrollmentCommandHandler;

    @Autowired
    public EnrollmentStreamCommandHandler(StreamBridge streamBridge,
            EnrollmentCommandHandler enrollmentCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.enrollmentCommandHandler = enrollmentCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Enrollment";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return enrollmentCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> enrollmentServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
