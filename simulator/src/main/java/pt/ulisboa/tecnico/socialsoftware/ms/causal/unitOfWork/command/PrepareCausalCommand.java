package pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

public class PrepareCausalCommand extends Command {
    private Aggregate aggregate;

    public PrepareCausalCommand(Integer aggregateId, String serviceName, Aggregate aggregate) {
        super(null, serviceName, aggregateId);
        this.aggregate = aggregate;
    }

    public Aggregate getAggregate() {
        return aggregate;
    }

    public void setAggregate(Aggregate aggregate) {
        this.aggregate = aggregate;
    }
}
