package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetStudentByExecutionIdAndUserIdCommand extends Command {
    private Integer executionAggregateId;
    private Integer userAggregateId;

    public GetStudentByExecutionIdAndUserIdCommand(UnitOfWork unitOfWork, String serviceName, Integer executionAggregateId, Integer userAggregateId) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }
}
