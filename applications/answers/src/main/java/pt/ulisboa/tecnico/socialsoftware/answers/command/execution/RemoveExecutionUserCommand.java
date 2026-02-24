package pt.ulisboa.tecnico.socialsoftware.answers.command.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class RemoveExecutionUserCommand extends Command {
    private final Integer executionId;
    private final Integer userAggregateId;

    public RemoveExecutionUserCommand(UnitOfWork unitOfWork, String serviceName, Integer executionId, Integer userAggregateId) {
        super(unitOfWork, serviceName, null);
        this.executionId = executionId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getExecutionId() { return executionId; }
    public Integer getUserAggregateId() { return userAggregateId; }
}
