package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetQuestionsByCourseExecutionIdCommand extends Command {
    private final Integer courseAggregateId;

    public GetQuestionsByCourseExecutionIdCommand(UnitOfWork unitOfWork, String serviceName, Integer courseAggregateId) {
        super(unitOfWork, serviceName, courseAggregateId);
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseAggregateId() { return courseAggregateId; }
}
