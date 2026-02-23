package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentParticipantDeletedEvent extends Event {
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