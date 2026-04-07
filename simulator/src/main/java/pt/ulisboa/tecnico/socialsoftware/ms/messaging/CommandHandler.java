package pt.ulisboa.tecnico.socialsoftware.ms.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public abstract class CommandHandler {
    @Autowired(required = false)
    private TransactionCommandHandler transactionCommandHandler;

    protected abstract String getAggregateTypeName();

    protected abstract Object handleDomainCommand(Command command);

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Object handle(Command command) {
        if (transactionCommandHandler != null) {
            return transactionCommandHandler.handle(command, this);
        }
        return handleDomainCommand(command);
    }
}
