package pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetTopicByIdCommand extends Command {
    private Integer topicAggregateId;

    public GetTopicByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer topicAggregateId) {
        super(unitOfWork, serviceName, topicAggregateId);
        this.topicAggregateId = topicAggregateId;
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }
}
