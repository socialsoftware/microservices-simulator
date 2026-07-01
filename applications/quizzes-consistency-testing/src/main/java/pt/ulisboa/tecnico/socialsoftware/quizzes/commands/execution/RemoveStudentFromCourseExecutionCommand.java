package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class RemoveStudentFromCourseExecutionCommand extends Command {
    private Integer courseExecutionAggregateId;
    private Integer userAggregateId;

    public RemoveStudentFromCourseExecutionCommand(UnitOfWork unitOfWork, String serviceName, Integer courseExecutionAggregateId, Integer userAggregateId) {
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
