package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class RemoveTopicCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer topicAggregateId;
    private final Integer eventVersion;

    public RemoveTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer topicAggregateId, Integer eventVersion) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.topicAggregateId = topicAggregateId;
        this.eventVersion = eventVersion;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }
}
