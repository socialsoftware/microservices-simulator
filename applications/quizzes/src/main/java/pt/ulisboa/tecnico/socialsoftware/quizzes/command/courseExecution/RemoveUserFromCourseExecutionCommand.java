package pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class RemoveUserFromCourseExecutionCommand extends Command {
    private Integer executionAggregateId;
    private Integer userAggregateId;
    private Integer aggregateEventVersion;

    public RemoveUserFromCourseExecutionCommand(UnitOfWork unitOfWork, String serviceName, Integer executionAggregateId, Integer userAggregateId, Integer aggregateEventVersion) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.userAggregateId = userAggregateId;
        this.aggregateEventVersion = aggregateEventVersion;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public Integer getAggregateEventVersion() {
        return aggregateEventVersion;
    }
}
