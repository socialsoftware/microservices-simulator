package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class AddParticipantCommand extends Command {
    private Integer tournamentAggregateId;
    private UserDto participantDto;

    public AddParticipantCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId, UserDto participantDto) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.participantDto = participantDto;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public UserDto getParticipant() {
        return participantDto;
    }

}
