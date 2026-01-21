package pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetAllCourseExecutionsCommand extends Command {
    public GetAllCourseExecutionsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }
}
