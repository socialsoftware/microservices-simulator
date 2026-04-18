package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.CreateCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.service.CourseService;

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
            case CreateCourseCommand cmd -> handleCreateCourse(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateCourse(CreateCourseCommand command) {
        return courseService.createCourse(command.getCourseDto(), command.getUnitOfWork());
    }
}
