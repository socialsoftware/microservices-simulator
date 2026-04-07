package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class PaymentOrderDeletedEvent extends Event {
    @Column(name = "payment_order_deleted_event_order_aggregate_id")
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