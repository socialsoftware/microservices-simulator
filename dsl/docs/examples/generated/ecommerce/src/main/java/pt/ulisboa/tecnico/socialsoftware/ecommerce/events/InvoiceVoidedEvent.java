package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class InvoiceVoidedEvent extends Event {
    private Integer orderAggregateId;
    private String invoiceNumber;

    public InvoiceVoidedEvent() {
        super();
    }

    public InvoiceVoidedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public InvoiceVoidedEvent(Integer aggregateId, Integer orderAggregateId, String invoiceNumber) {
        super(aggregateId);
        setOrderAggregateId(orderAggregateId);
        setInvoiceNumber(invoiceNumber);
    }

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }

    public void setOrderAggregateId(Integer orderAggregateId) {
        this.orderAggregateId = orderAggregateId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

}