package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class InvoiceOrderUpdatedEvent extends Event {
    private Integer orderAggregateId;
    private Integer orderVersion;

    public InvoiceOrderUpdatedEvent() {
        super();
    }

    public InvoiceOrderUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public InvoiceOrderUpdatedEvent(Integer aggregateId, Integer orderAggregateId, Integer orderVersion) {
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