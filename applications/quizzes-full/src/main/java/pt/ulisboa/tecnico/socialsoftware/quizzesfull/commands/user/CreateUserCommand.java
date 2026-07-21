package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;

public class CreateUserCommand extends Command {
    private final UserDto userDto;

    public CreateUserCommand(UnitOfWork unitOfWork, String serviceName, UserDto userDto) {
        super(unitOfWork, serviceName, null);
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
