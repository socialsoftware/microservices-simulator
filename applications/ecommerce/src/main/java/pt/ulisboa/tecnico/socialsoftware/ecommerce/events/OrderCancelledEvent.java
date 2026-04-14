package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderCancelledEvent extends Event {
    private Integer userAggregateId;
    private String reason;

    public OrderCancelledEvent() {
        super();
    }

    public OrderCancelledEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderCancelledEvent(Integer aggregateId, Integer userAggregateId, String reason) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setReason(reason);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}