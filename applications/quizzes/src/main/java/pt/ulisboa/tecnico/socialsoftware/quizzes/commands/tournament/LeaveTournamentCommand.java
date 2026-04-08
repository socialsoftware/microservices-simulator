package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

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
