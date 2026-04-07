package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class PaymentFailedEvent extends Event {
    private Integer orderAggregateId;
    private String reason;

    public PaymentFailedEvent() {
        super();
    }

    public PaymentFailedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PaymentFailedEvent(Integer aggregateId, Integer orderAggregateId, String reason) {
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