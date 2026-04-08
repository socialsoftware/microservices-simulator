package pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class GetAllCourseExecutionsCommand extends Command {
    public GetAllCourseExecutionsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }
}
