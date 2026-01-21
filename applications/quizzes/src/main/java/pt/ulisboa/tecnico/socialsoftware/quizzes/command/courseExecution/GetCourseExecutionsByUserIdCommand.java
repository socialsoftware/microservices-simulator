package pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetCourseExecutionsByUserIdCommand extends Command {
    private Integer userAggregateId;

    public GetCourseExecutionsByUserIdCommand(UnitOfWork unitOfWork, String serviceName, Integer userAggregateId) {
        super(unitOfWork, serviceName, userAggregateId);
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }
}
