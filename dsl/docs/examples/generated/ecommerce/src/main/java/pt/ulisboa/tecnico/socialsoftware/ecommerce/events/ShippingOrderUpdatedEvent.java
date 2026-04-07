package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ShippingOrderUpdatedEvent extends Event {
    @Column(name = "shipping_order_updated_event_order_aggregate_id")
    private Integer orderAggregateId;
    @Column(name = "shipping_order_updated_event_order_version")
    private Integer orderVersion;
    @Column(name = "shipping_order_updated_event_order_total_in_cents")
    private Double orderTotalInCents;
    @Column(name = "shipping_order_updated_event_order_item_count")
    private Integer orderItemCount;

    public ShippingOrderUpdatedEvent() {
        super();
    }

    public ShippingOrderUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ShippingOrderUpdatedEvent(Integer aggregateId, Integer orderAggregateId, Integer orderVersion, Double orderTotalInCents, Integer orderItemCount) {
        super(aggregateId);
        setOrderAggregateId(orderAggregateId);
        setOrderVersion(orderVersion);
        setOrderTotalInCents(orderTotalInCents);
        setOrderItemCount(orderItemCount);
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

    public Integer getOrderItemCount() {
        return orderItemCount;
    }

    public void setOrderItemCount(Integer orderItemCount) {
        this.orderItemCount = orderItemCount;
    }

}