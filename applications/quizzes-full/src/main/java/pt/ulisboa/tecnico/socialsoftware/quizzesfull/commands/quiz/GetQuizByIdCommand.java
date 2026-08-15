package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetQuizByIdCommand extends Command {
    private final Integer quizAggregateId;

    public GetQuizByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAggregateId) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getQuizAggregateId() { return quizAggregateId; }
}
