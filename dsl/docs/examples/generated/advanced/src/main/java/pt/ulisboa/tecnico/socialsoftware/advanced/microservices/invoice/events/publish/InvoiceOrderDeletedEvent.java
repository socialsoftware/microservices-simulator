package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class InvoiceOrderDeletedEvent extends Event {
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