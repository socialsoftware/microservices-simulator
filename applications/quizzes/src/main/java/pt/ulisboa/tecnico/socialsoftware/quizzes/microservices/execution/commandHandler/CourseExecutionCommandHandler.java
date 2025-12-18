package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;

import java.util.logging.Logger;

@Component
public class CourseExecutionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(CourseExecutionCommandHandler.class.getName());

    @Autowired
    private CourseExecutionService courseExecutionService;

    @Override
    protected String getAggregateTypeName() {
        return "CourseExecution";
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
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateCourseExecution(CreateCourseExecutionCommand command) {
        logger.info("Creating course execution: " + command.getCourseExecutionDto());
        try {
            CourseExecutionDto courseExecutionDto = courseExecutionService.createCourseExecution(
                    command.getCourseExecutionDto(),
                    command.getUnitOfWork());
            return courseExecutionDto;
        } catch (Exception e) {
            logger.severe("Failed to create course execution: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveCourseExecution(RemoveCourseExecutionCommand command) {
        logger.info("Removing course execution: " + command.getExecutionAggregateId());
        try {
            courseExecutionService.removeCourseExecution(
                    command.getExecutionAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove course execution: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveStudentFromCourseExecution(RemoveStudentFromCourseExecutionCommand command) {
        logger.info("Removing student from course execution: " + command.getCourseExecutionAggregateId() + ", "
                + command.getUserAggregateId());
        try {
            courseExecutionService.removeStudentFromCourseExecution(
                    command.getCourseExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove student from course execution: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateExecutionStudentName(UpdateExecutionStudentNameCommand command) {
        logger.info("Updating execution student name: " + command.getExecutionAggregateId() + ", "
                + command.getUserAggregateId() + ", "
                + command.getName());
        try {
            courseExecutionService.updateExecutionStudentName(
                    command.getExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getName(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to update execution student name: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetStudentByExecutionIdAndUserId(GetStudentByExecutionIdAndUserIdCommand command) {
        logger.info("Getting student by execution id and user id: " + command.getExecutionAggregateId() + ", "
                + command.getUserAggregateId());
        try {
            return courseExecutionService.getStudentByExecutionIdAndUserId(
                    command.getExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get student by execution id and user id: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetCourseExecutionsByUserId(GetCourseExecutionsByUserIdCommand command) {
        logger.info("Getting course executions by user id: " + command.getUserAggregateId());
        try {
            return courseExecutionService.getCourseExecutionsByUserId(
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get course executions by user id: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetCourseExecutionById(GetCourseExecutionByIdCommand command) {
        logger.info("Getting course execution by id: " + command.getExecutionAggregateId());
        try {
            return courseExecutionService.getCourseExecutionById(
                    command.getExecutionAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get course execution by id: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllCourseExecutions(GetAllCourseExecutionsCommand command) {
        logger.info("Getting all course executions");
        try {
            return courseExecutionService.getAllCourseExecutions(command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get all course executions: " + e.getMessage());
            return e;
        }
    }

    private Object handleEnrollStudent(EnrollStudentCommand command) {
        logger.info("Enrolling student: " + command.getUserDto().getAggregateId() + " in course execution: "
                + command.getCourseExecutionAggregateId());
        try {
            courseExecutionService.enrollStudent(
                    command.getCourseExecutionAggregateId(),
                    command.getUserDto(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to enroll student: " + e.getMessage());
            return e;
        }
    }

    private Object handleAnonymizeStudent(AnonymizeStudentCommand command) {
        logger.info(
                "Anonymizing student: " + command.getUserAggregateId() + " in course execution: "
                        + command.getExecutionAggregateId());
        try {
            courseExecutionService.anonymizeStudent(
                    command.getExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to anonymize student: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveUser(RemoveUserCommand command) {
        logger.info(
                "Removing user: " + command.getUserAggregateId() + " in course execution: "
                        + command.getCourseExecutionAggregateId());
        try {
            courseExecutionService.removeUser(
                    command.getCourseExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to anonymize student: " + e.getMessage());
            return e;
        }
    }
}
