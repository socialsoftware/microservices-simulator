package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.StreamCommandHandler;

import java.util.function.Consumer;
import java.util.logging.Logger;

@Component
public class CourseExecutionStreamCommandHandler extends StreamCommandHandler {

    private static final Logger logger = Logger.getLogger(CourseExecutionStreamCommandHandler.class.getName());
    private final CourseExecutionCommandHandler courseExecutionCommandHandler;

    @Autowired
    public CourseExecutionStreamCommandHandler(StreamBridge streamBridge, CourseExecutionCommandHandler courseExecutionCommandHandler) {
        super(streamBridge);
        this.courseExecutionCommandHandler = courseExecutionCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        logger.info("CourseExecutionStreamCommandHandler handling command: " + command.getServiceName());
        return courseExecutionCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<?>> courseExecutionServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
