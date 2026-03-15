package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.commandhandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.CreateCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

@Component
public class CourseCommandHandler extends CommandHandler {

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
            case CreateCourseCommand cmd -> handleCreateCourse(cmd);
            default -> null;
        };
    }

    private Object handleGetCourseById(GetCourseByIdCommand command) {
        return courseService.getCourseById(0, (SagaUnitOfWork) command.getUnitOfWork());
    }

    private Object handleCreateCourse(CreateCourseCommand command) {
        return courseService.createCourse((SagaUnitOfWork) command.getUnitOfWork());
    }
}
