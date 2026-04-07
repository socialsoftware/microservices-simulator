package pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class RemoveQuizAnswerCommand extends Command {
    private Integer quizAnswerAggregateId;

    public RemoveQuizAnswerCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAnswerAggregateId) {
        super(unitOfWork, serviceName, quizAnswerAggregateId);
        this.quizAnswerAggregateId = quizAnswerAggregateId;
    }

    public Integer getQuizAnswerAggregateId() { return quizAnswerAggregateId; }
}
