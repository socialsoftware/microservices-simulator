package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.command.execution.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;

import java.util.logging.Logger;

@Component
public class ExecutionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(ExecutionCommandHandler.class.getName());

    @Autowired
    private ExecutionService executionService;

    @Override
    protected String getAggregateTypeName() {
        return "Execution";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateExecutionCommand cmd -> handleCreateExecution(cmd);
            case GetExecutionByIdCommand cmd -> handleGetExecutionById(cmd);
            case GetAllExecutionsCommand cmd -> handleGetAllExecutions(cmd);
            case UpdateExecutionCommand cmd -> handleUpdateExecution(cmd);
            case DeleteExecutionCommand cmd -> handleDeleteExecution(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateExecution(CreateExecutionCommand cmd) {
        logger.info("handleCreateExecution");
        try {
            return executionService.createExecution(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetExecutionById(GetExecutionByIdCommand cmd) {
        logger.info("handleGetExecutionById");
        try {
            return executionService.getExecutionById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllExecutions(GetAllExecutionsCommand cmd) {
        logger.info("handleGetAllExecutions");
        try {
            return executionService.getAllExecutions(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateExecution(UpdateExecutionCommand cmd) {
        logger.info("handleUpdateExecution");
        try {
            return executionService.updateExecution(cmd.getExecutionDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteExecution(DeleteExecutionCommand cmd) {
        logger.info("handleDeleteExecution");
        try {
            executionService.deleteExecution(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
