package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentExecutionDeletedEvent extends Event {
    private Integer executionAggregateId;

    public TournamentExecutionDeletedEvent() {
        super();
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