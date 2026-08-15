package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetExecutionsCommand extends Command {

    protected GetExecutionsCommand() {}

    public GetExecutionsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);
    }
}
