package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetExecutionByIdCommand extends Command {
    private final Integer executionAggregateId;

    public GetExecutionByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer executionAggregateId) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionAggregateId() { return executionAggregateId; }
}
