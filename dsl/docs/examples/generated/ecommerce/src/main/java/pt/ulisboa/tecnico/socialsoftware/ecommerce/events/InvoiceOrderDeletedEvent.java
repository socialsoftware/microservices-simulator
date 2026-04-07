package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class InvoiceOrderDeletedEvent extends Event {
    @Column(name = "invoice_order_deleted_event_order_aggregate_id")
    private Integer orderAggregateId;

    public InvoiceOrderDeletedEvent() {
        super();
    }

    public InvoiceOrderDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public InvoiceOrderDeletedEvent(Integer aggregateId, Integer orderAggregateId) {
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