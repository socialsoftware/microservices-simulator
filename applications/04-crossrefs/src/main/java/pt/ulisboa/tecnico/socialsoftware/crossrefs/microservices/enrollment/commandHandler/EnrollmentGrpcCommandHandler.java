package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class EnrollmentGrpcCommandHandler extends GrpcCommandHandler {

    private final EnrollmentCommandHandler enrollmentCommandHandler;

    public EnrollmentGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            EnrollmentCommandHandler enrollmentCommandHandler) {
        super(mapperProvider);
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
}
