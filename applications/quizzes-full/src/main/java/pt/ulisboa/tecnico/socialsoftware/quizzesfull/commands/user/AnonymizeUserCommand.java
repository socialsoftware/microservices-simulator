package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class AnonymizeUserCommand extends Command {
    private final Integer userAggregateId;

    public AnonymizeUserCommand(UnitOfWork unitOfWork, String serviceName, Integer userAggregateId) {
        super(unitOfWork, serviceName, userAggregateId);
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserAggregateId() { return userAggregateId; }
}
