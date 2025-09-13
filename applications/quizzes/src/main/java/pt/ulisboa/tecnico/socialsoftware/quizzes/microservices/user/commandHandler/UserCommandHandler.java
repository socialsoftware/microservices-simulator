package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.user.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

import java.util.logging.Logger;

@Component
public class UserCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(UserCommandHandler.class.getName());

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        if (command instanceof GetUserByIdCommand) {
            returnObject = handleGetUserById((GetUserByIdCommand) command);
        } else if (command instanceof CreateUserCommand) {
            returnObject = handleCreateUser((CreateUserCommand) command);
        } else if (command instanceof ActivateUserCommand) {
            returnObject = handleActivateUser((ActivateUserCommand) command);
        } else if (command instanceof DeleteUserCommand) {
            returnObject = handleDeleteUser((DeleteUserCommand) command);
        } else if (command instanceof GetStudentsCommand) {
            returnObject = handleGetStudents((GetStudentsCommand) command);
        } else if (command instanceof GetTeachersCommand) {
            returnObject = handleGetTeachers((GetTeachersCommand) command);
        } else {
            logger.warning("Unknown command type: " + command.getClass().getName());
            returnObject = null;
        }
        if (command.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(command.getRootAggregateId(), command.getSemanticLock(),
                    (SagaUnitOfWork) command.getUnitOfWork());
        }
        return returnObject;
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
}
