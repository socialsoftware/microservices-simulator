package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.commandHandler;

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
public class LoanStreamCommandHandler extends StreamCommandHandler {

    private final LoanCommandHandler loanCommandHandler;

    @Autowired
    public LoanStreamCommandHandler(StreamBridge streamBridge,
            LoanCommandHandler loanCommandHandler,
            MessagingObjectMapperProvider mapperProvider) {
        super(streamBridge, mapperProvider);
        this.loanCommandHandler = loanCommandHandler;
    }

    @Override
    public String getAggregateTypeName() {
        return "Loan";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return loanCommandHandler.handleDomainCommand(command);
    }

    @Bean
    public Consumer<Message<?>> loanServiceCommandChannel() {
        return this::handleCommandMessage;
    }
}
