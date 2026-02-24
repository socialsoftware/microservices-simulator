package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class LeaveTournamentCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer userAggregateId;

    public LeaveTournamentCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer userAggregateId) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }
}
