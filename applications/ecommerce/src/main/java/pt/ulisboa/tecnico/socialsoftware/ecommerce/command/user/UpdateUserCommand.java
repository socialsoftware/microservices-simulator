package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;

public class UpdateUserCommand extends Command {
    private final UserDto userDto;

    public UpdateUserCommand(UnitOfWork unitOfWork, String serviceName, UserDto userDto) {
        super(unitOfWork, serviceName, null);
        this.userDto = userDto;
    }

    public UserDto getUserDto() { return userDto; }
}
