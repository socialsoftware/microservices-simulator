package pt.ulisboa.tecnico.socialsoftware.answers.command.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

public class UpdateTopicCommand extends Command {
    private final TopicDto topicDto;

    public UpdateTopicCommand(UnitOfWork unitOfWork, String serviceName, TopicDto topicDto) {
        super(unitOfWork, serviceName, null);
        this.topicDto = topicDto;
    }

    public TopicDto getTopicDto() { return topicDto; }
}
