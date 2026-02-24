package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;

@Component
@Profile("grpc")
public class CourseGrpcCommandHandler extends GrpcCommandHandler {

    private final CourseCommandHandler courseCommandHandler;

    public CourseGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            CourseCommandHandler courseCommandHandler) {
        super(mapperProvider);
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
}
