package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

/**
 * Fetches one participant of a tournament (their enrollment details as a
 * {@code UserDto}), failing when the user is not a participant.
 * <p>
 * Note the command is ABOUT the participant, but participants live inside the
 * Tournament aggregate — so handling it loads the tournament.
 */
public class GetTournamentParticipantCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer userAggregateId;

    public GetTournamentParticipantCommand(
            UnitOfWork unitOfWork, String serviceName,
            Integer tournamentAggregateId, Integer userAggregateId) {

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
