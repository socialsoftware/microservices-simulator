package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class CategoryGrpcCommandHandler extends GrpcCommandHandler {

    private final CategoryCommandHandler categoryCommandHandler;

    public CategoryGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            CategoryCommandHandler categoryCommandHandler) {
        super(mapperProvider);
        this.categoryCommandHandler = categoryCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Category";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return categoryCommandHandler.handleDomainCommand(command);
    }
}
