package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.user.CreateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.user.DeleteUserCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.user.GetUsersCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.user.UpdateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.service.UserService;

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
            case GetUserByIdCommand cmd -> handleGetUserById(cmd);
            case GetUsersCommand cmd -> handleGetUsers(cmd);
            case CreateUserCommand cmd -> handleCreateUser(cmd);
            case UpdateUserCommand cmd -> handleUpdateUser(cmd);
            case DeleteUserCommand cmd -> handleDeleteUser(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetUserById(GetUserByIdCommand command) {
        return userService.getUserById(command.getUserAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetUsers(GetUsersCommand command) {
        return userService.getUsers(command.getUnitOfWork());
    }

    private Object handleCreateUser(CreateUserCommand command) {
        return userService.createUser(command.getUserDto(), command.getUnitOfWork());
    }

    private Object handleUpdateUser(UpdateUserCommand command) {
        userService.updateUser(command.getUserAggregateId(), command.getUserDto(), command.getUnitOfWork());
        return null;
    }

    private Object handleDeleteUser(DeleteUserCommand command) {
        userService.deleteUser(command.getUserAggregateId(), command.getUnitOfWork());
        return null;
    }
}
