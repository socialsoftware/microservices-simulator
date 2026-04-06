package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class CartGrpcCommandHandler extends GrpcCommandHandler {

    private final CartCommandHandler cartCommandHandler;

    public CartGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            CartCommandHandler cartCommandHandler) {
        super(mapperProvider);
        this.cartCommandHandler = cartCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Cart";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return cartCommandHandler.handleDomainCommand(command);
    }
}
