package pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

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
