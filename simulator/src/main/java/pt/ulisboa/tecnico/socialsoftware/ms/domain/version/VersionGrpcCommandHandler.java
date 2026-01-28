package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;

@Component
@Profile("grpc")
public class VersionGrpcCommandHandler extends GrpcCommandHandler {

    private final VersionCommandHandler versionCommandHandler;

    public VersionGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            VersionCommandHandler versionCommandHandler) {
        super(mapperProvider);
        this.versionCommandHandler = versionCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Version";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        // Delegate to the standalone VersionCommandHandler
        return versionCommandHandler.handle(command);
    }
}
