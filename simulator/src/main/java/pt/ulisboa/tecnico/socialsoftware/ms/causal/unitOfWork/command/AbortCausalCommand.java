package pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class AbortCausalCommand extends Command {
    public AbortCausalCommand(Integer aggregateId, String serviceName) {
        super(null, serviceName, aggregateId);
    }
}
