package pt.ulisboa.tecnico.socialsoftware.answers.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;

public class AddTournamentParticipantCommand extends Command {
    private final Integer tournamentId;
    private final Integer participantAggregateId;
    private final TournamentParticipantDto participantDto;

    public AddTournamentParticipantCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto) {
        super(unitOfWork, serviceName, null);
        this.tournamentId = tournamentId;
        this.participantAggregateId = participantAggregateId;
        this.participantDto = participantDto;
    }

    public Integer getTournamentId() { return tournamentId; }
    public Integer getParticipantAggregateId() { return participantAggregateId; }
    public TournamentParticipantDto getParticipantDto() { return participantDto; }
}
