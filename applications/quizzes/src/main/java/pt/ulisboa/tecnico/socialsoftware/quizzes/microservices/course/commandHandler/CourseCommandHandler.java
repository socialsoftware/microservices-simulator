package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;

import java.util.logging.Logger;

@Component
public class CourseCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(CourseCommandHandler.class.getName());

    @Autowired
    private CourseService courseService;

    @Override
    public Object handle(Command command) {
        if (command instanceof GetCourseByIdCommand) {
            return handleGetCourseById((GetCourseByIdCommand) command);
        }

        logger.warning("Unknown command type: " + command.getClass().getName());
        return null;
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
}
