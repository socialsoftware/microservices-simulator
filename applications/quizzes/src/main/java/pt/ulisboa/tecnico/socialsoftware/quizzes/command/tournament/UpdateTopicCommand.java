package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class UpdateTopicCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer topicAggregateId;
    private final String topicName;
    private final Integer eventVersion;

    public UpdateTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer tournamentAggregateId,
            Integer topicAggregateId, String topicName, Integer eventVersion) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.topicAggregateId = topicAggregateId;
        this.topicName = topicName;
        this.eventVersion = eventVersion;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public String getTopicName() {
        return topicName;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }
}
