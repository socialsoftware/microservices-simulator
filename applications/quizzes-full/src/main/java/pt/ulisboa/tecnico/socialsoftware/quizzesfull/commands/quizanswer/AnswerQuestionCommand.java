package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class AnswerQuestionCommand extends Command {
    private final Integer quizAnswerAggregateId;
    private final Integer questionAggregateId;
    private final Long questionVersion;
    private final Integer optionKey;
    private final Integer timeTaken;

    public AnswerQuestionCommand(UnitOfWork unitOfWork, String serviceName,
                                 Integer quizAnswerAggregateId, Integer questionAggregateId,
                                 Long questionVersion, Integer optionKey, Integer timeTaken) {
        super(unitOfWork, serviceName, quizAnswerAggregateId);
        this.quizAnswerAggregateId = quizAnswerAggregateId;
        this.questionAggregateId = questionAggregateId;
        this.questionVersion = questionVersion;
        this.optionKey = optionKey;
        this.timeTaken = timeTaken;
    }

    public Integer getQuizAnswerAggregateId() { return quizAnswerAggregateId; }
    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public Long getQuestionVersion() { return questionVersion; }
    public Integer getOptionKey() { return optionKey; }
    public Integer getTimeTaken() { return timeTaken; }
}
