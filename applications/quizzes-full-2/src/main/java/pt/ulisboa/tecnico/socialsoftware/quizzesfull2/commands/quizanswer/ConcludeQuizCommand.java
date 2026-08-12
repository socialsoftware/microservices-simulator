package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class ConcludeQuizCommand extends Command {
    private Integer quizAnswerAggregateId;

    protected ConcludeQuizCommand() {}

    public ConcludeQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAnswerAggregateId) {
        super(unitOfWork, serviceName, quizAnswerAggregateId);
        this.quizAnswerAggregateId = quizAnswerAggregateId;
    }

    public Integer getQuizAnswerAggregateId() {
        return quizAnswerAggregateId;
    }

    public void setQuizAnswerAggregateId(Integer quizAnswerAggregateId) {
        this.quizAnswerAggregateId = quizAnswerAggregateId;
    }
}
