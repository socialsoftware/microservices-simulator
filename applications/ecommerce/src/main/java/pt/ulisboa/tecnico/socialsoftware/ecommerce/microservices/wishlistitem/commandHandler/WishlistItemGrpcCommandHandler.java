package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class WishlistItemGrpcCommandHandler extends GrpcCommandHandler {

    private final WishlistItemCommandHandler wishlistitemCommandHandler;

    public WishlistItemGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            WishlistItemCommandHandler wishlistitemCommandHandler) {
        super(mapperProvider);
        this.wishlistitemCommandHandler = wishlistitemCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "WishlistItem";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return wishlistitemCommandHandler.handleDomainCommand(command);
    }
}
