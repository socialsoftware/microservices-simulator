package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class UpdateUserNameCommand extends Command {
    private Integer userAggregateId;
    private String name;

    protected UpdateUserNameCommand() {}

    public UpdateUserNameCommand(UnitOfWork unitOfWork, String serviceName, Integer userAggregateId, String name) {
        super(unitOfWork, serviceName, userAggregateId);
        this.userAggregateId = userAggregateId;
        this.name = name;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
