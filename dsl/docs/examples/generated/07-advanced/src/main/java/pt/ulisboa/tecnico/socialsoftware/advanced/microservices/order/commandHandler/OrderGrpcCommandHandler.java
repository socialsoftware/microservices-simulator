package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class OrderGrpcCommandHandler extends GrpcCommandHandler {

    private final OrderCommandHandler orderCommandHandler;

    public OrderGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            OrderCommandHandler orderCommandHandler) {
        super(mapperProvider);
        this.orderCommandHandler = orderCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Order";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return orderCommandHandler.handleDomainCommand(command);
    }
}
