package pt.ulisboa.tecnico.socialsoftware.answers.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveQuestionTopicCommand extends Command {
    private final Integer questionId;
    private final Integer topicAggregateId;

    public RemoveQuestionTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer questionId, Integer topicAggregateId) {
        super(unitOfWork, serviceName, null);
        this.questionId = questionId;
        this.topicAggregateId = topicAggregateId;
    }

    public Integer getQuestionId() { return questionId; }
    public Integer getTopicAggregateId() { return topicAggregateId; }
}
