package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ShippingDispatchedEvent extends Event {
    private Integer orderAggregateId;
    private String trackingNumber;

    public ShippingDispatchedEvent() {
        super();
    }

    public ShippingDispatchedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ShippingDispatchedEvent(Integer aggregateId, Integer orderAggregateId, String trackingNumber) {
        super(aggregateId);
        setOrderAggregateId(orderAggregateId);
        setTrackingNumber(trackingNumber);
    }

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }

    public void setOrderAggregateId(Integer orderAggregateId) {
        this.orderAggregateId = orderAggregateId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

}