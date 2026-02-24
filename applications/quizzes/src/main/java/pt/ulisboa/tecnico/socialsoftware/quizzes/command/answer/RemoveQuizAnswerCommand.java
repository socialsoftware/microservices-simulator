package pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class RemoveQuizAnswerCommand extends Command {
    private Integer quizAnswerAggregateId;

    public RemoveQuizAnswerCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAnswerAggregateId) {
        super(unitOfWork, serviceName, quizAnswerAggregateId);
        this.quizAnswerAggregateId = quizAnswerAggregateId;
    }

    public Integer getQuizAnswerAggregateId() { return quizAnswerAggregateId; }
}
