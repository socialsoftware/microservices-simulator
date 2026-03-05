package pt.ulisboa.tecnico.socialsoftware.answers.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

public class UpdateTournamentCommand extends Command {
    private final TournamentDto tournamentDto;

    public UpdateTournamentCommand(UnitOfWork unitOfWork, String serviceName, TournamentDto tournamentDto) {
        super(unitOfWork, serviceName, null);
        this.tournamentDto = tournamentDto;
    }

    public TournamentDto getTournamentDto() { return tournamentDto; }
}
