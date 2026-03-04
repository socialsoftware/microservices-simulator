package pt.ulisboa.tecnico.socialsoftware.answers.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class DeleteQuestionCommand extends Command {


    public DeleteQuestionCommand(UnitOfWork unitOfWork, String serviceName, Integer aggregateId) {
        super(unitOfWork, serviceName, aggregateId);

    }


}
