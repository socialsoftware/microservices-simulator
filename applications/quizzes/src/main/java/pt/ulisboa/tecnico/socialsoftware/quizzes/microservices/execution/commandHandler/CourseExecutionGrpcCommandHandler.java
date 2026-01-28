package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;

@Component
@Profile("grpc")
public class CourseExecutionGrpcCommandHandler extends GrpcCommandHandler {

    private final CourseExecutionCommandHandler courseExecutionCommandHandler;

    public CourseExecutionGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            CourseExecutionCommandHandler courseExecutionCommandHandler) {
        super(mapperProvider);
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
}
