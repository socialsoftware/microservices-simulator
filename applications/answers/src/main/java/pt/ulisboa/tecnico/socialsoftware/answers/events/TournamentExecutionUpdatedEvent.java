package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentExecutionUpdatedEvent extends Event {
    @Column(name = "tournament_execution_updated_event_execution_aggregate_id")
    private Integer executionAggregateId;
    @Column(name = "tournament_execution_updated_event_execution_version")
    private Integer executionVersion;
    @Column(name = "tournament_execution_updated_event_execution_acronym")
    private String executionAcronym;

    public TournamentExecutionUpdatedEvent() {
        super();
    }

    public TournamentExecutionUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentExecutionUpdatedEvent(Integer aggregateId, Integer executionAggregateId, Integer executionVersion, String executionAcronym) {
        super(aggregateId);
        setExecutionAggregateId(executionAggregateId);
        setExecutionVersion(executionVersion);
        setExecutionAcronym(executionAcronym);
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Integer executionVersion) {
        this.executionVersion = executionVersion;
    }

    public String getExecutionAcronym() {
        return executionAcronym;
    }

    public void setExecutionAcronym(String executionAcronym) {
        this.executionAcronym = executionAcronym;
    }

}