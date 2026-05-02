package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class AnonymizeStudentInExecutionCommand extends Command {
    private final Integer executionAggregateId;
    private final Integer userId;

    public AnonymizeStudentInExecutionCommand(UnitOfWork unitOfWork, String serviceName,
                                              Integer executionAggregateId, Integer userId) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.userId = userId;
    }

    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public Integer getUserId() { return userId; }
}
