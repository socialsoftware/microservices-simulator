package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class PaymentOrderUpdatedEvent extends Event {
    @Column(name = "payment_order_updated_event_order_aggregate_id")
    private Integer orderAggregateId;
    @Column(name = "payment_order_updated_event_order_version")
    private Integer orderVersion;
    @Column(name = "payment_order_updated_event_order_total_in_cents")
    private Double orderTotalInCents;

    public PaymentOrderUpdatedEvent() {
        super();
    }

    public PaymentOrderUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PaymentOrderUpdatedEvent(Integer aggregateId, Integer orderAggregateId, Integer orderVersion, Double orderTotalInCents) {
        super(aggregateId);
        setOrderAggregateId(orderAggregateId);
        setOrderVersion(orderVersion);
        setOrderTotalInCents(orderTotalInCents);
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

    public Double getOrderTotalInCents() {
        return orderTotalInCents;
    }

    public void setOrderTotalInCents(Double orderTotalInCents) {
        this.orderTotalInCents = orderTotalInCents;
    }

}