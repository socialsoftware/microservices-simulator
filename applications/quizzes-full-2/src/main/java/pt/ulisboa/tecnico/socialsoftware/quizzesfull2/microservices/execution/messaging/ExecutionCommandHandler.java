package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetExecutionsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetUserExecutionsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.service.ExecutionService;

import java.util.logging.Logger;

@Component
public class ExecutionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(ExecutionCommandHandler.class.getName());

    @Autowired
    private ExecutionService executionService;

    @Override
    public String getAggregateTypeName() {
        return "Execution";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetExecutionByIdCommand cmd -> handleGetExecutionById(cmd);
            case GetExecutionsCommand cmd -> handleGetExecutions(cmd);
            case GetUserExecutionsCommand cmd -> handleGetUserExecutions(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetExecutionById(GetExecutionByIdCommand command) {
        return executionService.getExecutionById(command.getExecutionAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetExecutions(GetExecutionsCommand command) {
        return executionService.getExecutions(command.getUnitOfWork());
    }

    private Object handleGetUserExecutions(GetUserExecutionsCommand command) {
        return executionService.getUserExecutions(command.getUserAggregateId(), command.getUnitOfWork());
    }
}
