package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ShippingOrderDeletedEvent extends Event {
    @Column(name = "shipping_order_deleted_event_order_aggregate_id")
    private Integer orderAggregateId;

    public ShippingOrderDeletedEvent() {
        super();
    }

    public ShippingOrderDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ShippingOrderDeletedEvent(Integer aggregateId, Integer orderAggregateId) {
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