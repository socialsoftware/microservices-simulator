package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class PaymentGrpcCommandHandler extends GrpcCommandHandler {

    private final PaymentCommandHandler paymentCommandHandler;

    public PaymentGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            PaymentCommandHandler paymentCommandHandler) {
        super(mapperProvider);
        this.paymentCommandHandler = paymentCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Payment";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return paymentCommandHandler.handleDomainCommand(command);
    }
}
