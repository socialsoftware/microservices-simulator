package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class CustomerGrpcCommandHandler extends GrpcCommandHandler {

    private final CustomerCommandHandler customerCommandHandler;

    public CustomerGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            CustomerCommandHandler customerCommandHandler) {
        super(mapperProvider);
        this.customerCommandHandler = customerCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Customer";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return customerCommandHandler.handleDomainCommand(command);
    }
}
