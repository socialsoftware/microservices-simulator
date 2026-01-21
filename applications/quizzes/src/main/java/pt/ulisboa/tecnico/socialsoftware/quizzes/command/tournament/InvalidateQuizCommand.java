package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class InvalidateQuizCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer aggregateId;
    private final Integer aggregateVersion;

    public InvalidateQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer aggregateId, Integer aggregateVersion) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public Integer getAggregateVersion() {
        return aggregateVersion;
    }
}
