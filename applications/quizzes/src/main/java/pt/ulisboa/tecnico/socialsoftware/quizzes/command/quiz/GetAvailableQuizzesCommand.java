package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class GetAvailableQuizzesCommand extends Command {
    private Integer courseExecutionAggregateId;

    public GetAvailableQuizzesCommand(UnitOfWork unitOfWork, String serviceName, Integer courseExecutionAggregateId) {
        super(unitOfWork, serviceName, courseExecutionAggregateId);
        this.courseExecutionAggregateId = courseExecutionAggregateId;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }
}
