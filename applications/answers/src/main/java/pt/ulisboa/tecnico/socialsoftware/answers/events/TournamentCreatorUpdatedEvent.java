package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentCreatorUpdatedEvent extends Event {
    @Column(name = "tournament_creator_updated_event_executionuser_aggregate_id")
    private Integer executionuserAggregateId;
    @Column(name = "tournament_creator_updated_event_executionuser_version")
    private Integer executionuserVersion;

    public TournamentCreatorUpdatedEvent() {
        super();
    }

    public TournamentCreatorUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentCreatorUpdatedEvent(Integer aggregateId, Integer executionuserAggregateId, Integer executionuserVersion) {
        super(aggregateId);
        setExecutionuserAggregateId(executionuserAggregateId);
        setExecutionuserVersion(executionuserVersion);
    }

    public Integer getExecutionuserAggregateId() {
        return executionuserAggregateId;
    }

    public void setExecutionuserAggregateId(Integer executionuserAggregateId) {
        this.executionuserAggregateId = executionuserAggregateId;
    }

    public Integer getExecutionuserVersion() {
        return executionuserVersion;
    }

    public void setExecutionuserVersion(Integer executionuserVersion) {
        this.executionuserVersion = executionuserVersion;
    }

}