package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class CancelTournamentCommand extends Command {
    private final Integer tournamentAggregateId;

    public CancelTournamentCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }
}
