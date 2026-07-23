package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class UpdateUserNameCommand extends Command {
    private final Integer userAggregateId;
    private final String newName;

    public UpdateUserNameCommand(UnitOfWork unitOfWork, String serviceName, Integer userAggregateId, String newName) {
        super(unitOfWork, serviceName, userAggregateId);
        this.userAggregateId = userAggregateId;
        this.newName = newName;
    }

    public Integer getUserAggregateId() { return userAggregateId; }
    public String getNewName() { return newName; }
}
