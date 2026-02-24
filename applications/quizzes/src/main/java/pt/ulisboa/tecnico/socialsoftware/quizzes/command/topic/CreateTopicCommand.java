package pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

public class CreateTopicCommand extends Command {
    private TopicDto topicDto;
    private TopicCourse course;

    public CreateTopicCommand(UnitOfWork unitOfWork, String serviceName, TopicDto topicDto, TopicCourse course) {
        super(unitOfWork, serviceName, null);
        this.topicDto = topicDto;
        this.course = course;
    }

    public TopicDto getTopicDto() { return topicDto; }
    public TopicCourse getCourse() { return course; }
}
