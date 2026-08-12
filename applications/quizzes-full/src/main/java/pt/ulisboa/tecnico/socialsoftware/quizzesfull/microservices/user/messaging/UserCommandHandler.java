package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.AnonymizeUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.CreateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.DeleteUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.UpdateUserNameCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.service.UserService;

import java.util.logging.Logger;

@Component
public class UserCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(UserCommandHandler.class.getName());

    @Autowired
    private UserService userService;

    @Override
    public String getAggregateTypeName() {
        return "User";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetUserByIdCommand cmd -> userService.getUserById(cmd.getUserAggregateId(), cmd.getUnitOfWork());
            case CreateUserCommand cmd -> userService.createUser(cmd.getUserDto(), cmd.getUnitOfWork());
            case DeleteUserCommand cmd -> {
                userService.deleteUser(cmd.getUserAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            case UpdateUserNameCommand cmd -> {
                userService.updateUserName(cmd.getUserAggregateId(), cmd.getNewName(), cmd.getUnitOfWork());
                yield null;
            }
            case AnonymizeUserCommand cmd -> {
                userService.anonymizeUser(cmd.getUserAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
