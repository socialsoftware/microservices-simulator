package pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.command;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class AbortCausalCommand extends Command {
    public AbortCausalCommand(Integer aggregateId, String serviceName) {
        super(null, serviceName, aggregateId);
    }
}
