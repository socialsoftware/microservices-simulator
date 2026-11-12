package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetTopicsByCourseCommand extends Command {
    private Integer courseAggregateId;

    protected GetTopicsByCourseCommand() {}

    public GetTopicsByCourseCommand(UnitOfWork unitOfWork, String serviceName, Integer courseAggregateId) {
        super(unitOfWork, serviceName, null);
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }
}
