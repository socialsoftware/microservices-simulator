package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;

public class UpdateTopicCommand extends Command {
    private TopicDto topicDto;

    public UpdateTopicCommand(UnitOfWork unitOfWork, String serviceName, TopicDto topicDto) {
        super(unitOfWork, serviceName, topicDto.getAggregateId());
        this.topicDto = topicDto;
    }

    public TopicDto getTopicDto() { return topicDto; }
}
