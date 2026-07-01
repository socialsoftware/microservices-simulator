package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetAvailableQuizzesCommand extends Command {
    private Integer courseExecutionAggregateId;

    public GetAvailableQuizzesCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAggregateId, Integer courseExecutionAggregateId) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.courseExecutionAggregateId = courseExecutionAggregateId;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }
}
