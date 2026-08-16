package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetTeachersCommand extends Command {

    protected GetTeachersCommand() {}

    public GetTeachersCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }
}
