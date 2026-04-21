package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.courseexecution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class GetCourseExecutionByIdCommand extends Command {
    private Integer courseExecutionAggregateId;

    public GetCourseExecutionByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer courseExecutionAggregateId) {
        super(unitOfWork, serviceName, null);
        this.courseExecutionAggregateId = courseExecutionAggregateId;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }
}
