package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.CreateCourseRemoteCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.DeleteCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetAndOrCreateCourseRemoteCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByNameRemoteCommand;
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

    // TODO: cleanup
    private Object handleGetAndOrCreateCourseRemote(GetAndOrCreateCourseRemoteCommand command) {
        logger.info("Getting or creating course by ID: " + command.getCourseExecutionDto().getAggregateId());
        return courseService.getAndOrCreateCourseRemote(command.getCourseExecutionDto(), command.getUnitOfWork());
    }

    private Object handleGetCourseByNameRemote(GetCourseByNameRemoteCommand command) {
        logger.info("Getting course by name: " + command.getCourseExecutionDto().getName());
        try {
            return courseService.getCourseByNameRemote(command.getCourseExecutionDto(), command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get course by name: " + e.getMessage());
            return e;
        }
    }

    private Object handleCreateCourseRemote(CreateCourseRemoteCommand command) {
        logger.info("Creating course: " + command.getCourseExecutionDto().getName());
        try {
            return courseService.createCourseRemote(command.getCourseExecutionDto(), command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to create course: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteCourse(DeleteCourseCommand command) {
        logger.info("Deleting course: " + command.getCourseAggregateId());
        try {
            courseService.deleteCourse(command.getCourseAggregateId(), command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to delete course: " + e.getMessage());
            return e;
        }
    }
}
