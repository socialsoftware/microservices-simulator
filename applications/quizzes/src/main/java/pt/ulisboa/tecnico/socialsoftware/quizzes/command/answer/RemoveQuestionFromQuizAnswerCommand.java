package pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveQuestionFromQuizAnswerCommand extends Command {
    private Integer answerAggregateId;
    private Integer questionAggregateId;
    private Integer aggregateVersion;

    public RemoveQuestionFromQuizAnswerCommand(UnitOfWork unitOfWork, String serviceName, Integer answerAggregateId, Integer questionAggregateId, Integer aggregateVersion) {
        super(unitOfWork, serviceName, answerAggregateId);
        this.answerAggregateId = answerAggregateId;
        this.questionAggregateId = questionAggregateId;
        this.aggregateVersion = aggregateVersion;
    }

    public Integer getAnswerAggregateId() { return answerAggregateId; }
    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public Integer getAggregateVersion() { return aggregateVersion; }
}
