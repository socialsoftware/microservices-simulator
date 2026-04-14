package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
public class InvoiceUpdatedEvent extends Event {
    @Column(name = "invoice_updated_event_total_amount")
    private Double totalAmount;
    @Column(name = "invoice_updated_event_issued_at")
    private LocalDateTime issuedAt;
    @Column(name = "invoice_updated_event_paid")
    private Boolean paid;

    public InvoiceUpdatedEvent() {
        super();
    }

    public InvoiceUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public InvoiceUpdatedEvent(Integer aggregateId, Double totalAmount, LocalDateTime issuedAt, Boolean paid) {
        super(aggregateId);
        setTotalAmount(totalAmount);
        setIssuedAt(issuedAt);
        setPaid(paid);
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

}