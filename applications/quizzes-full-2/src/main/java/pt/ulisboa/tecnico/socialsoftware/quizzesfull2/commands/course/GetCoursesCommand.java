package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.course;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetCoursesCommand extends Command {

    protected GetCoursesCommand() {}

    public GetCoursesCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }
}
