package pt.ulisboa.tecnico.socialsoftware.quizzes.command.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
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
