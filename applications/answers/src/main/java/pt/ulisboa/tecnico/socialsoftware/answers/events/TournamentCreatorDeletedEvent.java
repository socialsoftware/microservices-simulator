package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentCreatorDeletedEvent extends Event {
    @Column(name = "tournament_creator_deleted_event_executionuser_aggregate_id")
    private Integer executionuserAggregateId;

    public TournamentCreatorDeletedEvent() {
        super();
    }

    public TournamentCreatorDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentCreatorDeletedEvent(Integer aggregateId, Integer executionuserAggregateId) {
        super(aggregateId);
        setExecutionuserAggregateId(executionuserAggregateId);
    }

    public Integer getExecutionuserAggregateId() {
        return executionuserAggregateId;
    }

    public void setExecutionuserAggregateId(Integer executionuserAggregateId) {
        this.executionuserAggregateId = executionuserAggregateId;
    }

}