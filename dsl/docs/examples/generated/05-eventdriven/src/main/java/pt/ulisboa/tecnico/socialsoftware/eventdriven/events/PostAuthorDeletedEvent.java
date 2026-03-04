package pt.ulisboa.tecnico.socialsoftware.eventdriven.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class PostAuthorDeletedEvent extends Event {
    private Integer authorAggregateId;

    public PostAuthorDeletedEvent() {
        super();
    }

    public PostAuthorDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PostAuthorDeletedEvent(Integer aggregateId, Integer authorAggregateId) {
        super(aggregateId);
        setAuthorAggregateId(authorAggregateId);
    }

    public Integer getAuthorAggregateId() {
        return authorAggregateId;
    }

    public void setAuthorAggregateId(Integer authorAggregateId) {
        this.authorAggregateId = authorAggregateId;
    }

}