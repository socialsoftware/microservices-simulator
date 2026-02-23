package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;

@Entity
public class InvoiceUpdatedEvent extends Event {
    private Double totalAmount;
    private LocalDateTime issuedAt;
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