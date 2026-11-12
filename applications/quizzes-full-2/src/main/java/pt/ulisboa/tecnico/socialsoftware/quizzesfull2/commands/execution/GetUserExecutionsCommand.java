package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetUserExecutionsCommand extends Command {
    private Integer userAggregateId;

    protected GetUserExecutionsCommand() {}

    public GetUserExecutionsCommand(UnitOfWork unitOfWork, String serviceName, Integer userAggregateId) {
        super(unitOfWork, serviceName, null);
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }
}
