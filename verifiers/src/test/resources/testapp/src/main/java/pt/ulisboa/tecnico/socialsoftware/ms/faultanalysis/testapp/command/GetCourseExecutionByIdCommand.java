package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetCourseExecutionByIdCommand extends Command {
    public GetCourseExecutionByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer aggregateId) {
        super(unitOfWork, serviceName, aggregateId);
    }
}
