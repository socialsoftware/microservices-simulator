package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class InvoiceIssuedEvent extends Event {
    private Integer orderAggregateId;
    private String invoiceNumber;
    private Double amountInCents;

    public InvoiceIssuedEvent() {
        super();
    }

    public InvoiceIssuedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public InvoiceIssuedEvent(Integer aggregateId, Integer orderAggregateId, String invoiceNumber, Double amountInCents) {
        super(aggregateId);
        setOrderAggregateId(orderAggregateId);
        setInvoiceNumber(invoiceNumber);
        setAmountInCents(amountInCents);
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

    public Double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Double amountInCents) {
        this.amountInCents = amountInCents;
    }

}