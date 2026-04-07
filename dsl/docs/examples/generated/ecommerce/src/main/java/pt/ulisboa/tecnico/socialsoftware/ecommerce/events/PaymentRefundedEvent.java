package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class PaymentRefundedEvent extends Event {
    private Integer orderAggregateId;
    private Double amountInCents;

    public PaymentRefundedEvent() {
        super();
    }

    public PaymentRefundedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PaymentRefundedEvent(Integer aggregateId, Integer orderAggregateId, Double amountInCents) {
        super(aggregateId);
        setOrderAggregateId(orderAggregateId);
        setAmountInCents(amountInCents);
    }

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }

    public void setOrderAggregateId(Integer orderAggregateId) {
        this.orderAggregateId = orderAggregateId;
    }

    public Double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Double amountInCents) {
        this.amountInCents = amountInCents;
    }

}