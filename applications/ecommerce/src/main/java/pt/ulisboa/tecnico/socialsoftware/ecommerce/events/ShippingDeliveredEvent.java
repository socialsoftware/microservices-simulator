package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ShippingDeliveredEvent extends Event {
    private Integer orderAggregateId;

    public ShippingDeliveredEvent() {
        super();
    }

    public ShippingDeliveredEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ShippingDeliveredEvent(Integer aggregateId, Integer orderAggregateId) {
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