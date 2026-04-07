package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class InvoiceGrpcCommandHandler extends GrpcCommandHandler {

    private final InvoiceCommandHandler invoiceCommandHandler;

    public InvoiceGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            InvoiceCommandHandler invoiceCommandHandler) {
        super(mapperProvider);
        this.invoiceCommandHandler = invoiceCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Invoice";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return invoiceCommandHandler.handleDomainCommand(command);
    }
}
