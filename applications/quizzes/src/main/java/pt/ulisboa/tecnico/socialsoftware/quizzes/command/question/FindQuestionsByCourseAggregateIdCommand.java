package pt.ulisboa.tecnico.socialsoftware.quizzes.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class FindQuestionsByCourseAggregateIdCommand extends Command {
    private Integer courseAggregateId;

    public FindQuestionsByCourseAggregateIdCommand(UnitOfWork unitOfWork, String serviceName, Integer courseAggregateId) {
        super(unitOfWork, serviceName, courseAggregateId);
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }
}
