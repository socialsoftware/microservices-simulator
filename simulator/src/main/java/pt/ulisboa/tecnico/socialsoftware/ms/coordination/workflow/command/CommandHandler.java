package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public abstract class CommandHandler {
    @Autowired
    private TransactionCommandHandler transactionCommandHandler;

    protected abstract String getAggregateTypeName();

    protected abstract Object handleDomainCommand(Command command);

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Object handle(Command command) {
        return transactionCommandHandler.handle(command, this);
    }
}
