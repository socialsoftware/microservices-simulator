package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.command.user.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;

import java.util.logging.Logger;

@Component
public class UserCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(UserCommandHandler.class.getName());

    @Autowired
    private UserService userService;

    @Override
    protected String getAggregateTypeName() {
        return "User";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateUserCommand cmd -> handleCreateUser(cmd);
            case GetUserByIdCommand cmd -> handleGetUserById(cmd);
            case GetAllUsersCommand cmd -> handleGetAllUsers(cmd);
            case UpdateUserCommand cmd -> handleUpdateUser(cmd);
            case DeleteUserCommand cmd -> handleDeleteUser(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateUser(CreateUserCommand cmd) {
        logger.info("handleCreateUser");
        try {
            return userService.createUser(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetUserById(GetUserByIdCommand cmd) {
        logger.info("handleGetUserById");
        try {
            return userService.getUserById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllUsers(GetAllUsersCommand cmd) {
        logger.info("handleGetAllUsers");
        try {
            return userService.getAllUsers(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateUser(UpdateUserCommand cmd) {
        logger.info("handleUpdateUser");
        try {
            return userService.updateUser(cmd.getUserDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteUser(DeleteUserCommand cmd) {
        logger.info("handleDeleteUser");
        try {
            userService.deleteUser(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
