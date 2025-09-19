package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.StreamCommandHandler;

import java.util.function.Consumer;

@Component
public class CourseStreamCommandHandler extends StreamCommandHandler {


    private final CourseCommandHandler courseCommandHandler;

    @Autowired
    public CourseStreamCommandHandler(StreamBridge streamBridge, CourseCommandHandler courseCommandHandler) {
        super(streamBridge);
        this.courseCommandHandler = courseCommandHandler;
    }

    @Override
    public Object handle(Command command) {
        return courseCommandHandler.handle(command);
    }

    @Bean
    public Consumer<Message<Command>> courseServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}

