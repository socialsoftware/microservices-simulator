package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.course.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;

import java.util.logging.Logger;

@Component
public class CourseCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(CourseCommandHandler.class.getName());

    @Autowired
    private CourseService courseService;

    @Override
    protected String getAggregateTypeName() {
        return "Course";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetCourseByIdCommand cmd -> handleGetCourseById(cmd);
            case GetAndOrCreateCourseRemoteCommand cmd -> handleGetAndOrCreateCourseRemote(cmd);
            case GetCourseByNameRemoteCommand cmd -> handleGetCourseByNameRemote(cmd);
            case CreateCourseRemoteCommand cmd -> handleCreateCourseRemote(cmd);
            case DeleteCourseCommand cmd -> handleDeleteCourse(cmd);
            case UpdateCourseQuestionCountCommand cmd -> handleUpdateCourseQuestionCount(cmd);
            case UpdateCourseExecutionCountCommand cmd -> handleUpdateCourseExecutionCount(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetCourseById(GetCourseByIdCommand command) {
        logger.info("Getting course by ID: " + command.getAggregateId());
        return courseService.getCourseById(command.getAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetAndOrCreateCourseRemote(GetAndOrCreateCourseRemoteCommand command) {
        logger.info("Getting or creating course by ID: " + command.getCourseExecutionDto().getAggregateId());
        return courseService.getAndOrCreateCourseRemote(command.getCourseExecutionDto(), command.getUnitOfWork());
    }

    private Object handleGetCourseByNameRemote(GetCourseByNameRemoteCommand command) {
        logger.info("Getting course by name: " + command.getCourseExecutionDto().getName());
        return courseService.getCourseByNameRemote(command.getCourseExecutionDto(), command.getUnitOfWork());
    }

    private Object handleCreateCourseRemote(CreateCourseRemoteCommand command) {
        logger.info("Creating course: " + command.getCourseExecutionDto().getName());
        return courseService.createCourseRemote(command.getCourseExecutionDto(), command.getUnitOfWork());
    }

    private Object handleDeleteCourse(DeleteCourseCommand command) {
        logger.info("Deleting course: " + command.getCourseAggregateId());
        courseService.deleteCourse(command.getCourseAggregateId(), command.getUnitOfWork());
        return null;
    }

    private Object handleUpdateCourseQuestionCount(UpdateCourseQuestionCountCommand command) {
        logger.info("Updating course question count for course: " + command.getCourseAggregateId() + " increment: " + command.isIncrement());
        if (command.isIncrement()) {
            courseService.incrementCourseQuestionCount(command.getCourseAggregateId(), command.getUnitOfWork());
        } else {
            courseService.decrementCourseQuestionCount(command.getCourseAggregateId(), command.getUnitOfWork());
        }
        return null;
    }

    private Object handleUpdateCourseExecutionCount(UpdateCourseExecutionCountCommand command) {
        logger.info("Updating course execution count for course: " + command.getCourseAggregateId() + " increment: " + command.isIncrement());
        if (command.isIncrement()) {
            courseService.incrementCourseExecutionCount(command.getCourseAggregateId(), command.getUnitOfWork());
        } else {
            courseService.decrementCourseExecutionCount(command.getCourseAggregateId(), command.getUnitOfWork());
        }
        return null;
    }
}
