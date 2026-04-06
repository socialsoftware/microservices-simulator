package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class ProductGrpcCommandHandler extends GrpcCommandHandler {

    private final ProductCommandHandler productCommandHandler;

    public ProductGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            ProductCommandHandler productCommandHandler) {
        super(mapperProvider);
        this.productCommandHandler = productCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Product";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return productCommandHandler.handleDomainCommand(command);
    }
}
