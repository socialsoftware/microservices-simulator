package pt.ulisboa.tecnico.socialsoftware.answers.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class GetTournamentTopicCommand extends Command {
    private final Integer tournamentId;
    private final Integer topicAggregateId;

    public GetTournamentTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentId, Integer topicAggregateId) {
        super(unitOfWork, serviceName, null);
        this.tournamentId = tournamentId;
        this.topicAggregateId = topicAggregateId;
    }

    public Integer getTournamentId() { return tournamentId; }
    public Integer getTopicAggregateId() { return topicAggregateId; }
}
