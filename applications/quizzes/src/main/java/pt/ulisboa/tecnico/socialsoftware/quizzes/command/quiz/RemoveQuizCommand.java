package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveQuizCommand extends Command {
    private final Integer quizAggregateId;

    public RemoveQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAggregateId) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }
}
