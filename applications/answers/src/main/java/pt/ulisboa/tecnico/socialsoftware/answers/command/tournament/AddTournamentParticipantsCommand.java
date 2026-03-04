package pt.ulisboa.tecnico.socialsoftware.answers.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import java.util.List;

public class AddTournamentParticipantsCommand extends Command {
    private final Integer tournamentId;
    private final List<TournamentParticipantDto> participantDtos;

    public AddTournamentParticipantsCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentId, List<TournamentParticipantDto> participantDtos) {
        super(unitOfWork, serviceName, null);
        this.tournamentId = tournamentId;
        this.participantDtos = participantDtos;
    }

    public Integer getTournamentId() { return tournamentId; }
    public List<TournamentParticipantDto> getParticipantDtos() { return participantDtos; }
}
