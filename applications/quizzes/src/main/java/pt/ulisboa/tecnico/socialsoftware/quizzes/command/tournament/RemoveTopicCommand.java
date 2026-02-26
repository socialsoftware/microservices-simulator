package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveTopicCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer topicAggregateId;
    private final Long eventVersion;

    public RemoveTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer topicAggregateId, Long eventVersion) {
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

    public Long getEventVersion() {
        return eventVersion;
    }
}
