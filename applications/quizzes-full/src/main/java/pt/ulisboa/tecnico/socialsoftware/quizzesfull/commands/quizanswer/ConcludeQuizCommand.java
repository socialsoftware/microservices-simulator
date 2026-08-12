package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class ConcludeQuizCommand extends Command {
    private final Integer quizAnswerAggregateId;

    public ConcludeQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAnswerAggregateId) {
        super(unitOfWork, serviceName, quizAnswerAggregateId);
        this.quizAnswerAggregateId = quizAnswerAggregateId;
    }

    public Integer getQuizAnswerAggregateId() { return quizAnswerAggregateId; }
}
