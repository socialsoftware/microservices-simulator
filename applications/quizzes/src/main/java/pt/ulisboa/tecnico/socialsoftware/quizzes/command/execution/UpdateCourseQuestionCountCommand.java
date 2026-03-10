package pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class UpdateCourseQuestionCountCommand extends Command {
    private final Integer courseExecutionAggregateId;
    private final boolean increment;

    public UpdateCourseQuestionCountCommand(UnitOfWork unitOfWork, String serviceName,
            Integer courseExecutionAggregateId, boolean increment) {
        super(unitOfWork, serviceName, courseExecutionAggregateId);
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        this.increment = increment;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }

    public boolean isIncrement() {
        return increment;
    }
}
