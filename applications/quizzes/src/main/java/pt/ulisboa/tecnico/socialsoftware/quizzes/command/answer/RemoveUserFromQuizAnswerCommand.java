package pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveUserFromQuizAnswerCommand extends Command {
    private Integer answerAggregateId;
    private Integer userAggregateId;
    private Integer aggregateVersion;

    public RemoveUserFromQuizAnswerCommand(UnitOfWork unitOfWork, String serviceName, Integer answerAggregateId, Integer userAggregateId, Integer aggregateVersion) {
        super(unitOfWork, serviceName, answerAggregateId);
        this.answerAggregateId = answerAggregateId;
        this.userAggregateId = userAggregateId;
        this.aggregateVersion = aggregateVersion;
    }

    public Integer getAnswerAggregateId() { return answerAggregateId; }
    public Integer getUserAggregateId() { return userAggregateId; }
    public Integer getAggregateVersion() { return aggregateVersion; }
}
