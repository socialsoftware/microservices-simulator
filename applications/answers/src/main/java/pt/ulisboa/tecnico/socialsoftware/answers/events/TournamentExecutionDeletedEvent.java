package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentExecutionDeletedEvent extends Event {
    @Column(name = "tournament_execution_deleted_event_execution_aggregate_id")
    private Integer executionAggregateId;

    public TournamentExecutionDeletedEvent() {
        super();
    }

    public TournamentExecutionDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentExecutionDeletedEvent(Integer aggregateId, Integer executionAggregateId) {
        super(aggregateId);
        setExecutionAggregateId(executionAggregateId);
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

}