package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class UpdateCourseCommand extends Command {
    private final Integer courseAggregateId;
    private final String name;
    private final String type;

    public UpdateCourseCommand(UnitOfWork unitOfWork, String serviceName,
                               Integer courseAggregateId, String name, String type) {
        super(unitOfWork, serviceName, courseAggregateId);
        this.courseAggregateId = courseAggregateId;
        this.name = name;
        this.type = type;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
