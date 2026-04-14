package pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetAllEnrollmentsCommand extends Command {


    public GetAllEnrollmentsCommand(UnitOfWork unitOfWork, String serviceName) {
        super(unitOfWork, serviceName, null);

    }


}
