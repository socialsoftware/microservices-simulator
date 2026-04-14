package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.subscribe.PaymentSubscribesOrderCancelled;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.events.subscribe.PaymentSubscribesOrderPlaced;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentOrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.PaymentStatus;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Payment extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "payment")
    private PaymentOrder order;
    private Double amountInCents;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String authorizationCode;
    private String paymentMethod;

    public Payment() {

    }

    public Payment(Integer aggregateId, PaymentDto paymentDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAmountInCents(paymentDto.getAmountInCents());
        setStatus(PaymentStatus.valueOf(paymentDto.getStatus()));
        setAuthorizationCode(paymentDto.getAuthorizationCode());
        setPaymentMethod(paymentDto.getPaymentMethod());
        setOrder(paymentDto.getOrder() != null ? new PaymentOrder(paymentDto.getOrder()) : null);
    }


    public Payment(Payment other) {
        super(other);
        setOrder(other.getOrder() != null ? new PaymentOrder(other.getOrder()) : null);
        setAmountInCents(other.getAmountInCents());
        setStatus(other.getStatus());
        setAuthorizationCode(other.getAuthorizationCode());
        setPaymentMethod(other.getPaymentMethod());
    }

    public PaymentOrder getOrder() {
        return order;
    }

    public void setOrder(PaymentOrder order) {
        this.order = order;
        if (this.order != null) {
            this.order.setPayment(this);
        }
    }

    public Double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Double amountInCents) {
        this.amountInCents = amountInCents;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            eventSubscriptions.add(new PaymentSubscribesOrderPlaced());
            eventSubscriptions.add(new PaymentSubscribesOrderCancelled());
        }
        return eventSubscriptions;
    }



    private boolean invariantRule0() {
        return amountInCents > 0.0;
    }

    private boolean invariantRule1() {
        return this.paymentMethod != null && this.paymentMethod.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Payment amount must be positive");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Payment method cannot be empty");
        }
    }

    public PaymentDto buildDto() {
        PaymentDto dto = new PaymentDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setOrder(getOrder() != null ? new PaymentOrderDto(getOrder()) : null);
        dto.setAmountInCents(getAmountInCents());
        dto.setStatus(getStatus() != null ? getStatus().name() : null);
        dto.setAuthorizationCode(getAuthorizationCode());
        dto.setPaymentMethod(getPaymentMethod());
        return dto;
    }
}