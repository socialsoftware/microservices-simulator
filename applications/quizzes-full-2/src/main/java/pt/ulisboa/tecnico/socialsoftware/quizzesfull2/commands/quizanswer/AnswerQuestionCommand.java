package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class AnswerQuestionCommand extends Command {
    private Integer quizAnswerAggregateId;
    private Integer questionAggregateId;
    private Integer optionSequenceChoice;
    private Integer optionKey;
    private Integer timeTaken;

    protected AnswerQuestionCommand() {}

    public AnswerQuestionCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAnswerAggregateId,
                                 Integer questionAggregateId, Integer optionSequenceChoice, Integer optionKey,
                                 Integer timeTaken) {
        super(unitOfWork, serviceName, quizAnswerAggregateId);
        this.quizAnswerAggregateId = quizAnswerAggregateId;
        this.questionAggregateId = questionAggregateId;
        this.optionSequenceChoice = optionSequenceChoice;
        this.optionKey = optionKey;
        this.timeTaken = timeTaken;
    }

    public Integer getQuizAnswerAggregateId() {
        return quizAnswerAggregateId;
    }

    public void setQuizAnswerAggregateId(Integer quizAnswerAggregateId) {
        this.quizAnswerAggregateId = quizAnswerAggregateId;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getOptionSequenceChoice() {
        return optionSequenceChoice;
    }

    public void setOptionSequenceChoice(Integer optionSequenceChoice) {
        this.optionSequenceChoice = optionSequenceChoice;
    }

    public Integer getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(Integer optionKey) {
        this.optionKey = optionKey;
    }

    public Integer getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Integer timeTaken) {
        this.timeTaken = timeTaken;
    }
}
