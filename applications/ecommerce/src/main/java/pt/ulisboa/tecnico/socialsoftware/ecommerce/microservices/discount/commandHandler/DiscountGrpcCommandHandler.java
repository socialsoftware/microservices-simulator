package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class DiscountGrpcCommandHandler extends GrpcCommandHandler {

    private final DiscountCommandHandler discountCommandHandler;

    public DiscountGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            DiscountCommandHandler discountCommandHandler) {
        super(mapperProvider);
        this.discountCommandHandler = discountCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Discount";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return discountCommandHandler.handleDomainCommand(command);
    }
}
