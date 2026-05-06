package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.CreateUserCommand;
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
            case CreateUserCommand cmd -> handleCreateUser(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateUser(CreateUserCommand command) {
        return userService.createUser(command.getUserDto(), command.getUnitOfWork());
    }
}
