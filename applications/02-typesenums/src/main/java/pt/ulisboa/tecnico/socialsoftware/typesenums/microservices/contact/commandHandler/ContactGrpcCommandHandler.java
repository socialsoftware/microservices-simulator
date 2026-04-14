package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class ContactGrpcCommandHandler extends GrpcCommandHandler {

    private final ContactCommandHandler contactCommandHandler;

    public ContactGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            ContactCommandHandler contactCommandHandler) {
        super(mapperProvider);
        this.contactCommandHandler = contactCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Contact";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return contactCommandHandler.handleDomainCommand(command);
    }
}
