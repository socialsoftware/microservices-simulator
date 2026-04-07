package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class PaymentUpdatedEvent extends Event {
    @Column(name = "payment_updated_event_amount_in_cents")
    private Double amountInCents;
    @Column(name = "payment_updated_event_authorization_code")
    private String authorizationCode;
    @Column(name = "payment_updated_event_payment_method")
    private String paymentMethod;

    public PaymentUpdatedEvent() {
        super();
    }

    public PaymentUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PaymentUpdatedEvent(Integer aggregateId, Double amountInCents, String authorizationCode, String paymentMethod) {
        super(aggregateId);
        setAmountInCents(amountInCents);
        setAuthorizationCode(authorizationCode);
        setPaymentMethod(paymentMethod);
    }

    public Double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Double amountInCents) {
        this.amountInCents = amountInCents;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

}