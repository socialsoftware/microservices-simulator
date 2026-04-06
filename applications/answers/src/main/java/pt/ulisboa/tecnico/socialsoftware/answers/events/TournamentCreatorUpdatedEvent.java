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
    @Column(name = "tournament_creator_updated_event_creator_name")
    private String creatorName;
    @Column(name = "tournament_creator_updated_event_creator_username")
    private String creatorUsername;

    public TournamentCreatorUpdatedEvent() {
        super();
    }

    public TournamentCreatorUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentCreatorUpdatedEvent(Integer aggregateId, Integer executionuserAggregateId, Integer executionuserVersion, String creatorName, String creatorUsername) {
        super(aggregateId);
        setExecutionuserAggregateId(executionuserAggregateId);
        setExecutionuserVersion(executionuserVersion);
        setCreatorName(creatorName);
        setCreatorUsername(creatorUsername);
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

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

}