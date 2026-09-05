package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.user;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto;

public class UpdateUserCommand extends Command {
    private Integer userAggregateId;
    private UserDto userDto;

    public UpdateUserCommand(UnitOfWork unitOfWork, String serviceName,
                             Integer userAggregateId, UserDto userDto) {
        super(unitOfWork, serviceName, userAggregateId);
        this.userAggregateId = userAggregateId;
        this.userDto = userDto;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
