package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class InvoiceCustomerDeletedEvent extends Event {
    @Column(name = "invoice_customer_deleted_event_customer_aggregate_id")
    private Integer customerAggregateId;

    public InvoiceCustomerDeletedEvent() {
        super();
    }

    public InvoiceCustomerDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public InvoiceCustomerDeletedEvent(Integer aggregateId, Integer customerAggregateId) {
        super(aggregateId);
        setCustomerAggregateId(customerAggregateId);
    }

    public Integer getCustomerAggregateId() {
        return customerAggregateId;
    }

    public void setCustomerAggregateId(Integer customerAggregateId) {
        this.customerAggregateId = customerAggregateId;
    }

}