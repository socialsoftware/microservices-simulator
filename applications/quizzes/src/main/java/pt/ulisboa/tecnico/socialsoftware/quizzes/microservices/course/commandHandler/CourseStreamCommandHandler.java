package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler;

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
public class CourseStreamCommandHandler extends StreamCommandHandler {

    private final CourseCommandHandler courseCommandHandler;

    @Autowired
    public CourseStreamCommandHandler(StreamBridge streamBridge,
            CourseCommandHandler courseCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.courseCommandHandler = courseCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Course";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return courseCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> courseServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
