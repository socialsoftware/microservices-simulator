package pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class RemoveUserFromQuizAnswerCommand extends Command {
    private Integer answerAggregateId;
    private Integer userAggregateId;
    private Long aggregateVersion;

    public RemoveUserFromQuizAnswerCommand(UnitOfWork unitOfWork, String serviceName, Integer answerAggregateId, Integer userAggregateId, Long aggregateVersion) {
        super(unitOfWork, serviceName, answerAggregateId);
        this.answerAggregateId = answerAggregateId;
        this.userAggregateId = userAggregateId;
        this.aggregateVersion = aggregateVersion;
    }

    public Integer getAnswerAggregateId() { return answerAggregateId; }
    public Integer getUserAggregateId() { return userAggregateId; }
    public Long getAggregateVersion() { return aggregateVersion; }
}
