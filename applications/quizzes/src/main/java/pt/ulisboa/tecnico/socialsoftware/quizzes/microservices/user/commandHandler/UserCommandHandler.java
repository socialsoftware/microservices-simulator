package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.*;
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
        try {
            return userService.getUserById(command.getAggregateId(), command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get user: " + e.getMessage());
            return e;
        }
    }

    private Object handleCreateUser(CreateUserCommand command) {
        logger.info("Creating user");
        try {
            return userService.createUser(command.getUserDto(), command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to create user: " + e.getMessage());
            return e;
        }
    }

    private Object handleActivateUser(ActivateUserCommand command) {
        logger.info("Activating user: " + command.getUserAggregateId());
        try {
            userService.activateUser(command.getUserAggregateId(), command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to activate user: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteUser(DeleteUserCommand command) {
        logger.info("Deleting user: " + command.getUserAggregateId());
        try {
            userService.deleteUser(command.getUserAggregateId(), command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to delete user: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetStudents(GetStudentsCommand command) {
        logger.info("Getting students");
        try {
            return userService.getStudents(command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get students: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetTeachers(GetTeachersCommand command) {
        logger.info("Getting teachers");
        try {
            return userService.getTeachers(command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get teachers: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeactivateUser(DeactivateUserCommand command) {
        logger.info("Deactivating user: " + command.getUserAggregateId());
        try {
            userService.deactivateUser(command.getUserAggregateId(), command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to deactivate user: " + e.getMessage());
            return e;
        }
    }
}
