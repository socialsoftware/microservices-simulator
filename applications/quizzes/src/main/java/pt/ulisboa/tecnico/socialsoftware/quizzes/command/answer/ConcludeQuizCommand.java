package pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class ConcludeQuizCommand extends Command {
    private Integer quizAggregateId;
    private Integer userAggregateId;

    public ConcludeQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAggregateId, Integer userAggregateId) {
        super(unitOfWork, serviceName, null);
        this.quizAggregateId = quizAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getQuizAggregateId() { return quizAggregateId; }
    public Integer getUserAggregateId() { return userAggregateId; }
}
