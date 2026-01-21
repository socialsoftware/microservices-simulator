package pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveUserCommand extends Command {
    private Integer courseExecutionAggregateId;
    private Integer userAggregateId;

    public RemoveUserCommand(UnitOfWork unitOfWork, String serviceName, Integer courseExecutionAggregateId, Integer userAggregateId) {
        super(unitOfWork, serviceName, courseExecutionAggregateId);
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }
}
