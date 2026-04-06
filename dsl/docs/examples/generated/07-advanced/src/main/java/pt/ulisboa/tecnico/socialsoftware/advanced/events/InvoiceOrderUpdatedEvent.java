package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class InvoiceOrderUpdatedEvent extends Event {
    @Column(name = "invoice_order_updated_event_order_aggregate_id")
    private Integer orderAggregateId;
    @Column(name = "invoice_order_updated_event_order_version")
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