package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class PaymentUpdatedEvent extends Event {
    private Double amount;
    private String paymentDate;

    public PaymentUpdatedEvent() {
        super();
    }

    public PaymentUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PaymentUpdatedEvent(Integer aggregateId, Double amount, String paymentDate) {
        super(aggregateId);
        setAmount(amount);
        setPaymentDate(paymentDate);
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

}
