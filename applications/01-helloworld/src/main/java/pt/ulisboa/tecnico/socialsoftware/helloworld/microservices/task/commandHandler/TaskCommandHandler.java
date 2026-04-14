package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.helloworld.command.task.*;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.service.TaskService;

import java.util.logging.Logger;

@Component
public class TaskCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TaskCommandHandler.class.getName());

    @Autowired
    private TaskService taskService;

    @Override
    protected String getAggregateTypeName() {
        return "Task";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateTaskCommand cmd -> handleCreateTask(cmd);
            case GetTaskByIdCommand cmd -> handleGetTaskById(cmd);
            case GetAllTasksCommand cmd -> handleGetAllTasks(cmd);
            case UpdateTaskCommand cmd -> handleUpdateTask(cmd);
            case DeleteTaskCommand cmd -> handleDeleteTask(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateTask(CreateTaskCommand cmd) {
        logger.info("handleCreateTask");
        try {
            return taskService.createTask(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetTaskById(GetTaskByIdCommand cmd) {
        logger.info("handleGetTaskById");
        try {
            return taskService.getTaskById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllTasks(GetAllTasksCommand cmd) {
        logger.info("handleGetAllTasks");
        try {
            return taskService.getAllTasks(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateTask(UpdateTaskCommand cmd) {
        logger.info("handleUpdateTask");
        try {
            return taskService.updateTask(cmd.getTaskDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteTask(DeleteTaskCommand cmd) {
        logger.info("handleDeleteTask");
        try {
            taskService.deleteTask(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
