package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class RemoveQuizCommand extends Command {
    private final Integer quizAggregateId;

    public RemoveQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAggregateId) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }
}
