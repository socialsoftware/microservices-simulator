package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class InvoiceUpdatedEvent extends Event {
    @Column(name = "invoice_updated_event_invoice_number")
    private String invoiceNumber;
    @Column(name = "invoice_updated_event_amount_in_cents")
    private Double amountInCents;
    @Column(name = "invoice_updated_event_issued_at")
    private String issuedAt;

    public InvoiceUpdatedEvent() {
        super();
    }

    public InvoiceUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public InvoiceUpdatedEvent(Integer aggregateId, String invoiceNumber, Double amountInCents, String issuedAt) {
        super(aggregateId);
        setInvoiceNumber(invoiceNumber);
        setAmountInCents(amountInCents);
        setIssuedAt(issuedAt);
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

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }

}