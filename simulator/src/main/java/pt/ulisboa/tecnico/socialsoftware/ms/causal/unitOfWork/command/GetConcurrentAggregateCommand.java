package pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class GetConcurrentAggregateCommand extends Command {
    private Integer version;

    public GetConcurrentAggregateCommand(Integer aggregateId, String serviceName, Integer version) {
        super(null, serviceName, aggregateId);
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
