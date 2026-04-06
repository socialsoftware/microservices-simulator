package pt.ulisboa.tecnico.socialsoftware.eventdriven.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class PostAuthorDeletedEvent extends Event {
    @Column(name = "post_author_deleted_event_author_aggregate_id")
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