package pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

public class UpdateTopicCommand extends Command {
    private TopicDto topicDto;

    public UpdateTopicCommand(UnitOfWork unitOfWork, String serviceName, TopicDto topicDto) {
        super(unitOfWork, serviceName, topicDto.getAggregateId());
        this.topicDto = topicDto;
    }

    public TopicDto getTopicDto() { return topicDto; }
}
