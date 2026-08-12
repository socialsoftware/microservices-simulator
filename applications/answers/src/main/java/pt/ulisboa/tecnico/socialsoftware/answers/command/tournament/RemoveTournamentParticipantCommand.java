package pt.ulisboa.tecnico.socialsoftware.answers.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveTournamentParticipantCommand extends Command {
    private final Integer tournamentId;
    private final Integer participantAggregateId;

    public RemoveTournamentParticipantCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentId, Integer participantAggregateId) {
        super(unitOfWork, serviceName, null);
        this.tournamentId = tournamentId;
        this.participantAggregateId = participantAggregateId;
    }

    public Integer getTournamentId() { return tournamentId; }
    public Integer getParticipantAggregateId() { return participantAggregateId; }
}
