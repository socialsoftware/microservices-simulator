package pt.ulisboa.tecnico.socialsoftware.eventdriven.command.author;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class GetAuthorByIdCommand extends Command {


    public GetAuthorByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer aggregateId) {
        super(unitOfWork, serviceName, aggregateId);

    }


}
