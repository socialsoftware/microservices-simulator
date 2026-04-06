package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class BookGrpcCommandHandler extends GrpcCommandHandler {

    private final BookCommandHandler bookCommandHandler;

    public BookGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            BookCommandHandler bookCommandHandler) {
        super(mapperProvider);
        this.bookCommandHandler = bookCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Book";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return bookCommandHandler.handleDomainCommand(command);
    }
}
