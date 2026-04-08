package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.course;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class UpdateCourseQuestionCountCommand extends Command {
    private final Integer courseAggregateId;
    private final boolean increment;

    public UpdateCourseQuestionCountCommand(UnitOfWork unitOfWork, String serviceName,
            Integer courseAggregateId, boolean increment) {
        super(unitOfWork, serviceName, courseAggregateId);
        this.courseAggregateId = courseAggregateId;
        this.increment = increment;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public boolean isIncrement() {
        return increment;
    }
}
