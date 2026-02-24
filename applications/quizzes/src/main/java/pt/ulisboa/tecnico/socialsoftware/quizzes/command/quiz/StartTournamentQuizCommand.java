package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class StartTournamentQuizCommand extends Command {
    private Integer userAggregateId;
    private Integer quizAggregateId;

    public StartTournamentQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer userAggregateId, Integer quizAggregateId) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.userAggregateId = userAggregateId;
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getUserAggregateId() { return userAggregateId; }
    public Integer getQuizAggregateId() { return quizAggregateId; }
}
