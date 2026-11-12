package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;

public class CreateTopicCommand extends Command {
    private TopicDto topicDto;

    protected CreateTopicCommand() {}

    public CreateTopicCommand(UnitOfWork unitOfWork, String serviceName, TopicDto topicDto) {
        super(unitOfWork, serviceName, null);
        this.topicDto = topicDto;
    }

    public TopicDto getTopicDto() {
        return topicDto;
    }

    public void setTopicDto(TopicDto topicDto) {
        this.topicDto = topicDto;
    }
}
