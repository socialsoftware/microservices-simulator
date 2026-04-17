package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;

public class CreateTopicCommand extends Command {
    private TopicDto topicDto;

    public CreateTopicCommand(UnitOfWork unitOfWork, String serviceName, TopicDto topicDto) {
        super(unitOfWork, serviceName, null);
        this.topicDto = topicDto;
    }

    public TopicDto getTopicDto() {
        return topicDto;
    }
}
