package pt.ulisboa.tecnico.socialsoftware.eventdriven.command.author;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetAllAuthorsCommand extends Command {


    public GetAllAuthorsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);

    }


}
