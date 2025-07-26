package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class SolveQuizCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer userAggregateId;
    private final Integer answerAggregateId;

    public SolveQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer userAggregateId, Integer answerAggregateId) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.userAggregateId = userAggregateId;
        this.answerAggregateId = answerAggregateId;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public Integer getAnswerAggregateId() {
        return answerAggregateId;
    }
}
