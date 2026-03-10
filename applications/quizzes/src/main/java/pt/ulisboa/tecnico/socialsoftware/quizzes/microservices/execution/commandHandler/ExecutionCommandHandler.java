package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.ExecutionService;

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
            case CreateCourseExecutionCommand cmd -> handleCreateCourseExecution(cmd);
            case RemoveCourseExecutionCommand cmd -> handleRemoveCourseExecution(cmd);
            case RemoveStudentFromCourseExecutionCommand cmd -> handleRemoveStudentFromCourseExecution(cmd);
            case UpdateExecutionStudentNameCommand cmd -> handleUpdateExecutionStudentName(cmd);
            case GetStudentByExecutionIdAndUserIdCommand cmd -> handleGetStudentByExecutionIdAndUserId(cmd);
            case GetCourseExecutionsByUserIdCommand cmd -> handleGetCourseExecutionsByUserId(cmd);
            case GetCourseExecutionByIdCommand cmd -> handleGetCourseExecutionById(cmd);
            case GetAllCourseExecutionsCommand cmd -> handleGetAllCourseExecutions(cmd);
            case EnrollStudentCommand cmd -> handleEnrollStudent(cmd);
            case AnonymizeStudentCommand cmd -> handleAnonymizeStudent(cmd);
            case RemoveUserCommand cmd -> handleRemoveUser(cmd);
            case UpdateCourseQuestionCountCommand cmd -> handleUpdateCourseQuestionCount(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleUpdateCourseQuestionCount(UpdateCourseQuestionCountCommand command) {
        logger.info("Updating course question count for execution: " + command.getCourseExecutionAggregateId()
                + " increment: " + command.isIncrement());
        if (command.isIncrement()) {
            executionService.incrementCourseQuestionCount(command.getCourseExecutionAggregateId(),
                    command.getUnitOfWork());
        } else {
            executionService.decrementCourseQuestionCount(command.getCourseExecutionAggregateId(),
                    command.getUnitOfWork());
        }
        return null;
    }

    private Object handleCreateCourseExecution(CreateCourseExecutionCommand command) {
        logger.info("Creating course execution: " + command.getCourseExecutionDto());
        return executionService.createCourseExecution(
                command.getCourseExecutionDto(),
                command.getUnitOfWork());
    }

    private Object handleRemoveCourseExecution(RemoveCourseExecutionCommand command) {
        logger.info("Removing course execution: " + command.getExecutionAggregateId());
        executionService.removeCourseExecution(
                command.getExecutionAggregateId(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleRemoveStudentFromCourseExecution(RemoveStudentFromCourseExecutionCommand command) {
        logger.info("Removing student from course execution: " + command.getCourseExecutionAggregateId() + ", "
                + command.getUserAggregateId());
        executionService.removeStudentFromCourseExecution(
                command.getCourseExecutionAggregateId(),
                command.getUserAggregateId(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleUpdateExecutionStudentName(UpdateExecutionStudentNameCommand command) {
        logger.info("Updating execution student name: " + command.getExecutionAggregateId() + ", "
                + command.getUserAggregateId() + ", "
                + command.getName());
        executionService.updateExecutionStudentName(
                command.getExecutionAggregateId(),
                command.getUserAggregateId(),
                command.getName(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleGetStudentByExecutionIdAndUserId(GetStudentByExecutionIdAndUserIdCommand command) {
        logger.info("Getting student by execution id and user id: " + command.getExecutionAggregateId() + ", "
                + command.getUserAggregateId());
        return executionService.getStudentByExecutionIdAndUserId(
                command.getExecutionAggregateId(),
                command.getUserAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleGetCourseExecutionsByUserId(GetCourseExecutionsByUserIdCommand command) {
        logger.info("Getting course executions by user id: " + command.getUserAggregateId());
        return executionService.getCourseExecutionsByUserId(
                command.getUserAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleGetCourseExecutionById(GetCourseExecutionByIdCommand command) {
        logger.info("Getting course execution by id: " + command.getExecutionAggregateId());
        return executionService.getCourseExecutionById(
                command.getExecutionAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleGetAllCourseExecutions(GetAllCourseExecutionsCommand command) {
        logger.info("Getting all course executions");
        return executionService.getAllCourseExecutions(command.getUnitOfWork());
    }

    private Object handleEnrollStudent(EnrollStudentCommand command) {
        logger.info("Enrolling student: " + command.getUserDto().getAggregateId() + " in course execution: "
                + command.getCourseExecutionAggregateId());
        executionService.enrollStudent(
                command.getCourseExecutionAggregateId(),
                command.getUserDto(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleAnonymizeStudent(AnonymizeStudentCommand command) {
        logger.info(
                "Anonymizing student: " + command.getUserAggregateId() + " in course execution: "
                        + command.getExecutionAggregateId());
        executionService.anonymizeStudent(
                command.getExecutionAggregateId(),
                command.getUserAggregateId(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleRemoveUser(RemoveUserCommand command) {
        logger.info(
                "Removing user: " + command.getUserAggregateId() + " in course execution: "
                        + command.getCourseExecutionAggregateId());
        executionService.removeUser(
                command.getCourseExecutionAggregateId(),
                command.getUserAggregateId(),
                command.getUnitOfWork());
        return null;
    }
}
