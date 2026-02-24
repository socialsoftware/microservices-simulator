package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class CommitSagaCommand extends Command {
    private Integer aggregateId;

    public CommitSagaCommand(Integer aggregateId, String serviceName) {
        super(null, serviceName, aggregateId);
        this.aggregateId = aggregateId;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }
}
