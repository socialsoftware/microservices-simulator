package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class TaskGrpcCommandHandler extends GrpcCommandHandler {

    private final TaskCommandHandler taskCommandHandler;

    public TaskGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            TaskCommandHandler taskCommandHandler) {
        super(mapperProvider);
        this.taskCommandHandler = taskCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Task";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return taskCommandHandler.handleDomainCommand(command);
    }
}
