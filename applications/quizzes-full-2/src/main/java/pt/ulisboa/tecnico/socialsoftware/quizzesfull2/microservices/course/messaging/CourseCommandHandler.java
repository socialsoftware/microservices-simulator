package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.course.GetCoursesCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.service.CourseService;

import java.util.logging.Logger;

@Component
public class CourseCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(CourseCommandHandler.class.getName());

    @Autowired
    private CourseService courseService;

    @Override
    public String getAggregateTypeName() {
        return "Course";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetCourseByIdCommand cmd -> handleGetCourseById(cmd);
            case GetCoursesCommand cmd -> handleGetCourses(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetCourseById(GetCourseByIdCommand command) {
        return courseService.getCourseById(command.getCourseAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetCourses(GetCoursesCommand command) {
        return courseService.getCourses(command.getUnitOfWork());
    }
}
