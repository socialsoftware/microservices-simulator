package pt.ulisboa.tecnico.socialsoftware.quizzes.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class UpdateTopicCommand extends Command {
    private Integer questionAggregateId;
    private Integer topicAggregateId;
    private String topicName;
    private Integer aggregateVersion;

    public UpdateTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer questionAggregateId, Integer topicAggregateId, String topicName, Integer aggregateVersion) {
        super(unitOfWork, serviceName, questionAggregateId);
        this.questionAggregateId = questionAggregateId;
        this.topicAggregateId = topicAggregateId;
        this.topicName = topicName;
        this.aggregateVersion = aggregateVersion;
    }

    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public Integer getTopicAggregateId() { return topicAggregateId; }
    public String getTopicName() { return topicName; }
    public Integer getAggregateVersion() { return aggregateVersion; }
}
