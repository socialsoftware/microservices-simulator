package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class DeleteTopicCommand extends Command {
    private Integer topicAggregateId;

    protected DeleteTopicCommand() {}

    public DeleteTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer topicAggregateId) {
        super(unitOfWork, serviceName, topicAggregateId);
        this.topicAggregateId = topicAggregateId;
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public void setTopicAggregateId(Integer topicAggregateId) {
        this.topicAggregateId = topicAggregateId;
    }
}
