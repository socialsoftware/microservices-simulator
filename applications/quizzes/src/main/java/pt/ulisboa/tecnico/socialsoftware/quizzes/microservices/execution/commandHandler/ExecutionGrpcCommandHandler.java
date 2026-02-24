package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;

@Component
@Profile("grpc")
public class ExecutionGrpcCommandHandler extends GrpcCommandHandler {

    private final ExecutionCommandHandler executionCommandHandler;

    public ExecutionGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
                                       ExecutionCommandHandler executionCommandHandler) {
        super(mapperProvider);
        this.executionCommandHandler = executionCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Execution";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return executionCommandHandler.handleDomainCommand(command);
    }
}
