package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class TeacherGrpcCommandHandler extends GrpcCommandHandler {

    private final TeacherCommandHandler teacherCommandHandler;

    public TeacherGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            TeacherCommandHandler teacherCommandHandler) {
        super(mapperProvider);
        this.teacherCommandHandler = teacherCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Teacher";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return teacherCommandHandler.handleDomainCommand(command);
    }
}
