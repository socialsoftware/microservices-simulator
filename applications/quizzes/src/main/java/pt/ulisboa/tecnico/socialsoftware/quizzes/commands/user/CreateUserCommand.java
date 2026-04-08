package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.user;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class CreateUserCommand extends Command {
    private UserDto userDto;

    public CreateUserCommand(UnitOfWork unitOfWork, String serviceName, UserDto userDto) {
        super(unitOfWork, serviceName, null);
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
