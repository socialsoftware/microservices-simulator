package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentParticipantDeletedEvent extends Event {
    @Column(name = "tournament_participant_deleted_event_executionuser_aggregate_id")
    private Integer executionuserAggregateId;

    public TournamentParticipantDeletedEvent() {
        super();
    }

    public TournamentParticipantDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentParticipantDeletedEvent(Integer aggregateId, Integer executionuserAggregateId) {
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