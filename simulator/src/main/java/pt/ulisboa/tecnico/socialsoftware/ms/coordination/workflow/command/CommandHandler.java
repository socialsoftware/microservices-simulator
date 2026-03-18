package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public abstract class CommandHandler {
    @Autowired(required = false)
    private TransactionCommandHandler transactionCoordinator;

    protected abstract String getAggregateTypeName();

    protected abstract Object handleDomainCommand(Command command);

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Object handle(Command command) {
        if (transactionCoordinator != null) {
            return transactionCoordinator.handle(command, this);
        }

        return handleDomainCommand(command);
    }
}
