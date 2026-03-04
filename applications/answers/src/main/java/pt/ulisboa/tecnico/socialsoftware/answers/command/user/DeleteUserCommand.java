package pt.ulisboa.tecnico.socialsoftware.answers.command.user;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class DeleteUserCommand extends Command {


    public DeleteUserCommand(UnitOfWork unitOfWork, String serviceName, Integer aggregateId) {
        super(unitOfWork, serviceName, aggregateId);

    }


}
