package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp_variants.commandhandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.CreateCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

// C3: handleCreateCourse calls a READ service (courseExecutionService) for validation first,
// then calls the WRITE service (courseService). CommandHandlerVisitor uses findFirstServiceCall,
// so the READ call wins the race and CreateCourseCommand is incorrectly mapped as READ.
@Component
public class CourseCommandHandlerValidationFirst extends CommandHandler {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseExecutionService courseExecutionService;

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
        // Validation call first (READ) — this wins the "first service call" race
        courseExecutionService.getCourseExecutionById(0, null);
        // Actual write second
        return courseService.createCourse((SagaUnitOfWork) command.getUnitOfWork());
    }
}
