package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler;

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
public class CourseExecutionStreamCommandHandler extends StreamCommandHandler {

    private final CourseExecutionCommandHandler courseExecutionCommandHandler;

    @Autowired
    public CourseExecutionStreamCommandHandler(StreamBridge streamBridge,
            CourseExecutionCommandHandler courseExecutionCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.courseExecutionCommandHandler = courseExecutionCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "CourseExecution";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return courseExecutionCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> courseExecutionServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
