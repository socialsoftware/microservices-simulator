package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderCancelledEvent extends Event {
    private Integer userAggregateId;

    public OrderCancelledEvent() {
        super();
    }

    public OrderCancelledEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderCancelledEvent(Integer aggregateId, Integer userAggregateId) {
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