package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class OrderUserDeletedEvent extends Event {
    @Column(name = "order_user_deleted_event_user_aggregate_id")
    private Integer userAggregateId;

    public OrderUserDeletedEvent() {
        super();
    }

    public OrderUserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderUserDeletedEvent(Integer aggregateId, Integer userAggregateId) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

}