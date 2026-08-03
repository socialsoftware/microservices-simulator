package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.ActivateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.AnonymizeUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.CreateUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.DeleteUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetStudentsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetTeachersCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.UpdateUserNameCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.service.UserService;

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
            case GetStudentsCommand cmd -> handleGetStudents(cmd);
            case GetTeachersCommand cmd -> handleGetTeachers(cmd);
            case CreateUserCommand cmd -> handleCreateUser(cmd);
            case ActivateUserCommand cmd -> handleActivateUser(cmd);
            case UpdateUserNameCommand cmd -> handleUpdateUserName(cmd);
            case AnonymizeUserCommand cmd -> handleAnonymizeUser(cmd);
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

    private Object handleGetStudents(GetStudentsCommand command) {
        return userService.getStudents(command.getUnitOfWork());
    }

    private Object handleGetTeachers(GetTeachersCommand command) {
        return userService.getTeachers(command.getUnitOfWork());
    }

    private Object handleCreateUser(CreateUserCommand command) {
        return userService.createUser(command.getUserDto(), command.getUnitOfWork());
    }

    private Object handleActivateUser(ActivateUserCommand command) {
        userService.activateUser(command.getUserAggregateId(), command.getUnitOfWork());
        return null;
    }

    private Object handleUpdateUserName(UpdateUserNameCommand command) {
        userService.updateUserName(command.getUserAggregateId(), command.getName(), command.getUnitOfWork());
        return null;
    }

    private Object handleAnonymizeUser(AnonymizeUserCommand command) {
        userService.anonymizeUser(command.getUserAggregateId(), command.getUnitOfWork());
        return null;
    }

    private Object handleDeleteUser(DeleteUserCommand command) {
        userService.deleteUser(command.getUserAggregateId(), command.getUnitOfWork());
        return null;
    }
}
