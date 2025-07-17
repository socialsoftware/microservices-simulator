package pt.ulisboa.tecnico.socialsoftware.quizzes.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class RemoveTopicCommand extends Command {
    private Integer questionAggregateId;
    private Integer topicAggregateId;
    private Integer aggregateVersion;

    public RemoveTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer questionAggregateId, Integer topicAggregateId, Integer aggregateVersion) {
        super(unitOfWork, serviceName, questionAggregateId);
        this.questionAggregateId = questionAggregateId;
        this.topicAggregateId = topicAggregateId;
        this.aggregateVersion = aggregateVersion;
    }

    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public Integer getTopicAggregateId() { return topicAggregateId; }
    public Integer getAggregateVersion() { return aggregateVersion; }
}
