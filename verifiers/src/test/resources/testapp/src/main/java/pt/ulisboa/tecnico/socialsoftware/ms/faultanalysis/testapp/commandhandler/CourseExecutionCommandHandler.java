package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.commandhandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.CreateCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

@Component
public class CourseExecutionCommandHandler extends CommandHandler {

    @Autowired
    private CourseExecutionService courseExecutionService;

    @Override
    protected String getAggregateTypeName() {
        return "CourseExecution";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetCourseExecutionByIdCommand cmd -> handleGetCourseExecutionById(cmd);
            case CreateCourseExecutionCommand cmd -> handleCreateCourseExecution(cmd);
            default -> null;
        };
    }

    private Object handleGetCourseExecutionById(GetCourseExecutionByIdCommand command) {
        return courseExecutionService.getCourseExecutionById(0, (SagaUnitOfWork) command.getUnitOfWork());
    }

    private Object handleCreateCourseExecution(CreateCourseExecutionCommand command) {
        return courseExecutionService.createCourseExecution((SagaUnitOfWork) command.getUnitOfWork());
    }
}
