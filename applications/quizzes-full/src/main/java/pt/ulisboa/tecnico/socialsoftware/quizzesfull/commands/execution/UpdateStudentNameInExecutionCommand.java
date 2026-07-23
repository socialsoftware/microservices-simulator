package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class UpdateStudentNameInExecutionCommand extends Command {
    private final Integer executionAggregateId;
    private final Integer userId;
    private final String name;

    public UpdateStudentNameInExecutionCommand(UnitOfWork unitOfWork, String serviceName,
                                               Integer executionAggregateId, Integer userId, String name) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.userId = userId;
        this.name = name;
    }

    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public Integer getUserId() { return userId; }
    public String getName() { return name; }
}
