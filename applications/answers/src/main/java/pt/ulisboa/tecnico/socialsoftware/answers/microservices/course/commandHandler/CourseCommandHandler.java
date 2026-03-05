package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.command.course.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;

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
            case CreateCourseCommand cmd -> handleCreateCourse(cmd);
            case GetCourseByIdCommand cmd -> handleGetCourseById(cmd);
            case GetAllCoursesCommand cmd -> handleGetAllCourses(cmd);
            case UpdateCourseCommand cmd -> handleUpdateCourse(cmd);
            case DeleteCourseCommand cmd -> handleDeleteCourse(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateCourse(CreateCourseCommand cmd) {
        logger.info("handleCreateCourse");
        try {
            return courseService.createCourse(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetCourseById(GetCourseByIdCommand cmd) {
        logger.info("handleGetCourseById");
        try {
            return courseService.getCourseById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllCourses(GetAllCoursesCommand cmd) {
        logger.info("handleGetAllCourses");
        try {
            return courseService.getAllCourses(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateCourse(UpdateCourseCommand cmd) {
        logger.info("handleUpdateCourse");
        try {
            return courseService.updateCourse(cmd.getCourseDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteCourse(DeleteCourseCommand cmd) {
        logger.info("handleDeleteCourse");
        try {
            courseService.deleteCourse(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
