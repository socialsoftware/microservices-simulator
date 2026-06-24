package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.courseexecution.CreateCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.courseexecution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.service.CourseExecutionService;

import java.util.logging.Logger;

@Component
public class CourseExecutionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(CourseExecutionCommandHandler.class.getName());

    @Autowired
    private CourseExecutionService courseExecutionService;

    @Override
    public String getAggregateTypeName() {
        return "CourseExecution";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateCourseExecutionCommand cmd -> courseExecutionService.createCourseExecution(
                    cmd.getCourseExecutionDto(), cmd.getUnitOfWork());
            case GetCourseExecutionByIdCommand cmd -> courseExecutionService.getCourseExecutionById(
                    cmd.getCourseExecutionAggregateId(), cmd.getUnitOfWork());
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
