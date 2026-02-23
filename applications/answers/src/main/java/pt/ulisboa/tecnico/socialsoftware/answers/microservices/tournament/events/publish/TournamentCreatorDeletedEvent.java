package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentCreatorDeletedEvent extends Event {
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