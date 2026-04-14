package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.StreamCommandHandler;

import java.util.function.Consumer;

@Component
@Profile("stream")
public class PaymentStreamCommandHandler extends StreamCommandHandler {

    private final PaymentCommandHandler paymentCommandHandler;

    @Autowired
    public PaymentStreamCommandHandler(StreamBridge streamBridge,
            PaymentCommandHandler paymentCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
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

    @Bean
    public Consumer<Message<?>> paymentServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
