package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.user.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

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
            case GetUserByIdCommand cmd -> handleGetUserById(cmd);
            case CreateUserCommand cmd -> handleCreateUser(cmd);
            case ActivateUserCommand cmd -> handleActivateUser(cmd);
            case DeleteUserCommand cmd -> handleDeleteUser(cmd);
            case GetStudentsCommand cmd -> handleGetStudents(cmd);
            case GetTeachersCommand cmd -> handleGetTeachers(cmd);
            case DeactivateUserCommand cmd -> handleDeactivateUser(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetUserById(GetUserByIdCommand command) {
        logger.info("Getting user by ID: " + command.getAggregateId());
        return userService.getUserById(command.getAggregateId(), command.getUnitOfWork());
    }

    private Object handleCreateUser(CreateUserCommand command) {
        logger.info("Creating user");
        return userService.createUser(command.getUserDto(), command.getUnitOfWork());
    }

    private Object handleActivateUser(ActivateUserCommand command) {
        logger.info("Activating user: " + command.getUserAggregateId());
        userService.activateUser(command.getUserAggregateId(), command.getUnitOfWork());
        return null;
    }

    private Object handleDeleteUser(DeleteUserCommand command) {
        logger.info("Deleting user: " + command.getUserAggregateId());
        userService.deleteUser(command.getUserAggregateId(), command.getUnitOfWork());
        return null;
    }

    private Object handleGetStudents(GetStudentsCommand command) {
        logger.info("Getting students");
        return userService.getStudents(command.getUnitOfWork());
    }

    private Object handleGetTeachers(GetTeachersCommand command) {
        logger.info("Getting teachers");
        return userService.getTeachers(command.getUnitOfWork());
    }

    private Object handleDeactivateUser(DeactivateUserCommand command) {
        logger.info("Deactivating user: " + command.getUserAggregateId());
        userService.deactivateUser(command.getUserAggregateId(), command.getUnitOfWork());
        return null;
    }
}
