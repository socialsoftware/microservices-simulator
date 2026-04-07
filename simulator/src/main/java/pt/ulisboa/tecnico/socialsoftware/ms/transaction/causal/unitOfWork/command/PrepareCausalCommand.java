package pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.unitOfWork.command;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;

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
