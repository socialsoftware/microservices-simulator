package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetStudentsCommand extends Command {

    protected GetStudentsCommand() {}

    public GetStudentsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }
}
