package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ShippingFailedEvent extends Event {
    private Integer orderAggregateId;
    private String reason;

    public ShippingFailedEvent() {
        super();
    }

    public ShippingFailedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ShippingFailedEvent(Integer aggregateId, Integer orderAggregateId, String reason) {
        super(aggregateId);
        setOrderAggregateId(orderAggregateId);
        setReason(reason);
    }

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }

    public void setOrderAggregateId(Integer orderAggregateId) {
        this.orderAggregateId = orderAggregateId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}