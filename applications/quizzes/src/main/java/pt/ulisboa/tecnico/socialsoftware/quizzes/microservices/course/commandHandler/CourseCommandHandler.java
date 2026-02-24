package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetAndOrCreateCourseRemoteCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
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
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetCourseById(GetCourseByIdCommand command) {
        logger.info("Getting course by ID: " + command.getAggregateId());
        try {
            CourseDto courseDto = courseService.getCourseById(
                    command.getAggregateId(),
                    command.getUnitOfWork());
            return courseDto;
        } catch (Exception e) {
            logger.severe("Failed to get course by ID: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAndOrCreateCourseRemote(GetAndOrCreateCourseRemoteCommand command) {
        logger.info("Getting or creating course by ID: " + command.getCourseExecutionDto().getAggregateId());
        try {
            return courseService.getAndOrCreateCourseRemote(command.getCourseExecutionDto(), command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get or create course remote by ID: " + e.getMessage());
            return e;
        }
    }
}
