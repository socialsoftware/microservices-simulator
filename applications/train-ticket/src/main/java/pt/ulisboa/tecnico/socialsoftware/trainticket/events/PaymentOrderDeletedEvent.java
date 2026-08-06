package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class PaymentOrderDeletedEvent extends Event {
    private Integer orderAggregateId;

    public PaymentOrderDeletedEvent() {
        super();
    }

    public PaymentOrderDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PaymentOrderDeletedEvent(Integer aggregateId, Integer orderAggregateId) {
        super(aggregateId);
        setOrderAggregateId(orderAggregateId);
    }

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }

    public void setOrderAggregateId(Integer orderAggregateId) {
        this.orderAggregateId = orderAggregateId;
    }

}
