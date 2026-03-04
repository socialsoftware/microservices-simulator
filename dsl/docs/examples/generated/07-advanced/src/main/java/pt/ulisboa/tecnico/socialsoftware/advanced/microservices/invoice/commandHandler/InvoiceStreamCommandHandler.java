package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.StreamCommandHandler;

import java.util.function.Consumer;

@Component
@Profile("stream")
public class InvoiceStreamCommandHandler extends StreamCommandHandler {

    private final InvoiceCommandHandler invoiceCommandHandler;

    @Autowired
    public InvoiceStreamCommandHandler(StreamBridge streamBridge,
            InvoiceCommandHandler invoiceCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
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

    @Bean
    public Consumer<Message<?>> invoiceServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
