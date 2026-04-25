package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.CreateCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.DecrementExecutionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.DecrementQuestionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.DeleteCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.UpdateCourseCommand;
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
            case GetCourseByIdCommand cmd -> courseService.getCourseById(
                    cmd.getCourseAggregateId(), cmd.getUnitOfWork());
            case CreateCourseCommand cmd -> courseService.createCourse(
                    cmd.getName(), cmd.getType(), cmd.getUnitOfWork());
            case UpdateCourseCommand cmd -> {
                courseService.updateCourse(
                        cmd.getCourseAggregateId(), cmd.getName(), cmd.getType(), cmd.getUnitOfWork());
                yield null;
            }
            case DeleteCourseCommand cmd -> {
                courseService.deleteCourse(cmd.getCourseAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            case DecrementExecutionCountCommand cmd -> {
                courseService.decrementExecutionCount(cmd.getCourseAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            case DecrementQuestionCountCommand cmd -> {
                courseService.decrementQuestionCount(cmd.getCourseAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
