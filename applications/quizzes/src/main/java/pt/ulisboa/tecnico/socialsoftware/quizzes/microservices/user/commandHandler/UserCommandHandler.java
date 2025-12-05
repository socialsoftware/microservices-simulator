package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.CommitSagaCommand;
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
        switch (command) {
            case GetUserByIdCommand getUserByIdCommand -> returnObject = handleGetUserById(getUserByIdCommand);
            case CreateUserCommand createUserCommand -> returnObject = handleCreateUser(createUserCommand);
            case ActivateUserCommand activateUserCommand -> returnObject = handleActivateUser(activateUserCommand);
            case DeleteUserCommand deleteUserCommand -> returnObject = handleDeleteUser(deleteUserCommand);
            case GetStudentsCommand getStudentsCommand -> returnObject = handleGetStudents(getStudentsCommand);
            case GetTeachersCommand getTeachersCommand -> returnObject = handleGetTeachers(getTeachersCommand);
            case DeactivateUserCommand deactivateUserCommand ->
                returnObject = handleDeactivateUser(deactivateUserCommand);
            case CommitSagaCommand commitSagaCommand -> returnObject = handleCommitSaga(commitSagaCommand);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                returnObject = null;
            }
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

    private Object handleCommitSaga(CommitSagaCommand command) {
        logger.info("Committing saga for aggregate: " + command.getAggregateId());
        try {
            sagaUnitOfWorkService.commitAggregate(command.getAggregateId());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to commit saga: " + e.getMessage());
            return e;
        }
    }
}
