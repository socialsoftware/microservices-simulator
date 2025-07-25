package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentParticipant;

public class AddParticipantCommand extends Command {
    private Integer tournamentAggregateId;
    private TournamentParticipant participant;

    public AddParticipantCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId, TournamentParticipant participant) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.participant = participant;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public TournamentParticipant getParticipant() {
        return participant;
    }

}
