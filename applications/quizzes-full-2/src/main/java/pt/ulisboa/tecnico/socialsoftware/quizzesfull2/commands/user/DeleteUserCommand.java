package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class DeleteUserCommand extends Command {
    private Integer userAggregateId;

    protected DeleteUserCommand() {}

    public DeleteUserCommand(UnitOfWork unitOfWork, String serviceName, Integer userAggregateId) {
        super(unitOfWork, serviceName, userAggregateId);
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }
}
