package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class LoanGrpcCommandHandler extends GrpcCommandHandler {

    private final LoanCommandHandler loanCommandHandler;

    public LoanGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            LoanCommandHandler loanCommandHandler) {
        super(mapperProvider);
        this.loanCommandHandler = loanCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Loan";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return loanCommandHandler.handleDomainCommand(command);
    }
}
