package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class ShippingGrpcCommandHandler extends GrpcCommandHandler {

    private final ShippingCommandHandler shippingCommandHandler;

    public ShippingGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            ShippingCommandHandler shippingCommandHandler) {
        super(mapperProvider);
        this.shippingCommandHandler = shippingCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Shipping";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return shippingCommandHandler.handleDomainCommand(command);
    }
}
