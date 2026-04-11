package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.commandHandler;

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
public class TeacherStreamCommandHandler extends StreamCommandHandler {

    private final TeacherCommandHandler teacherCommandHandler;

    @Autowired
    public TeacherStreamCommandHandler(StreamBridge streamBridge,
            TeacherCommandHandler teacherCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.teacherCommandHandler = teacherCommandHandler;
    }

    @Override
    public String getAggregateTypeName() {
        return "Teacher";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return teacherCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> teacherServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
