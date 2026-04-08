package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class RemoveQuestionFromQuizAnswerCommand extends Command {
    private Integer answerAggregateId;
    private Integer questionAggregateId;
    private Long aggregateVersion;

    public RemoveQuestionFromQuizAnswerCommand(UnitOfWork unitOfWork, String serviceName, Integer answerAggregateId, Integer questionAggregateId, Long aggregateVersion) {
        super(unitOfWork, serviceName, answerAggregateId);
        this.answerAggregateId = answerAggregateId;
        this.questionAggregateId = questionAggregateId;
        this.aggregateVersion = aggregateVersion;
    }

    public Integer getAnswerAggregateId() { return answerAggregateId; }
    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public Long getAggregateVersion() { return aggregateVersion; }
}
