package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class DeleteQuestionCommand extends Command {
    private final Integer questionAggregateId;

    public DeleteQuestionCommand(UnitOfWork unitOfWork, String serviceName, Integer questionAggregateId) {
        super(unitOfWork, serviceName, questionAggregateId);
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getQuestionAggregateId() { return questionAggregateId; }
}
