package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class UpdateTopicCommand extends Command {
    private Integer topicAggregateId;
    private String name;

    protected UpdateTopicCommand() {}

    public UpdateTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer topicAggregateId, String name) {
        super(unitOfWork, serviceName, topicAggregateId);
        this.topicAggregateId = topicAggregateId;
        this.name = name;
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public void setTopicAggregateId(Integer topicAggregateId) {
        this.topicAggregateId = topicAggregateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
