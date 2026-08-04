package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class PaymentOrderUpdatedEvent extends Event {
    private Integer orderAggregateId;
    private Integer orderVersion;

    public PaymentOrderUpdatedEvent() {
        super();
    }

    public PaymentOrderUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PaymentOrderUpdatedEvent(Integer aggregateId, Integer orderAggregateId, Integer orderVersion) {
        super(aggregateId);
        setOrderAggregateId(orderAggregateId);
        setOrderVersion(orderVersion);
    }

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }

    public void setOrderAggregateId(Integer orderAggregateId) {
        this.orderAggregateId = orderAggregateId;
    }

    public Integer getOrderVersion() {
        return orderVersion;
    }

    public void setOrderVersion(Integer orderVersion) {
        this.orderVersion = orderVersion;
    }

}
