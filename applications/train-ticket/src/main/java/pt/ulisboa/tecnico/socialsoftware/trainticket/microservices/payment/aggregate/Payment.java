package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate;

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

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.events.subscribe.PaymentSubscribesOrderDeletedPaymentOrderExists;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.events.subscribe.PaymentSubscribesUserDeletedPaymentUserExists;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentOrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentUserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.PaymentType;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Payment extends Aggregate {
    private Double amount;
    @Enumerated(EnumType.STRING)
    private PaymentType type;
    private String paymentDate;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "payment")
    private PaymentOrder order;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "payment")
    private PaymentUser user;

    public Payment() {

    }

    public Payment(Integer aggregateId, PaymentDto paymentDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAmount(paymentDto.getAmount());
        setType(PaymentType.valueOf(paymentDto.getType()));
        setPaymentDate(paymentDto.getPaymentDate());
        setOrder(paymentDto.getOrder() != null ? new PaymentOrder(paymentDto.getOrder()) : null);
        setUser(paymentDto.getUser() != null ? new PaymentUser(paymentDto.getUser()) : null);
    }


    public Payment(Payment other) {
        super(other);
        setAmount(other.getAmount());
        setType(other.getType());
        setPaymentDate(other.getPaymentDate());
        setOrder(new PaymentOrder(other.getOrder()));
        setUser(new PaymentUser(other.getUser()));
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public PaymentType getType() {
        return type;
    }

    public void setType(PaymentType type) {
        this.type = type;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
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

    public PaymentUser getUser() {
        return user;
    }

    public void setUser(PaymentUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setPayment(this);
        }
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantPaymentOrderExists(eventSubscriptions);
            interInvariantPaymentUserExists(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantPaymentOrderExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new PaymentSubscribesOrderDeletedPaymentOrderExists(this.getOrder()));
    }

    private void interInvariantPaymentUserExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new PaymentSubscribesUserDeletedPaymentUserExists(this.getUser()));
    }


    private boolean invariantAmountPositive() {
        return amount > 0;
    }

    private boolean invariantTypeSet() {
        return this.type != null;
    }

    private boolean invariantPaymentDateSet() {
        return this.paymentDate != null && this.paymentDate != null && this.paymentDate.length() > 0;
    }

    private boolean invariantOrderSet() {
        return this.order != null;
    }

    private boolean invariantUserSet() {
        return this.user != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantAmountPositive()) {
            throw new SimulatorException(INVARIANT_BREAK, "Payment amount must be positive");
        }
        if (!invariantTypeSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Payment must have a type");
        }
        if (!invariantPaymentDateSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Payment must have a date");
        }
        if (!invariantOrderSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Payment must reference an order");
        }
        if (!invariantUserSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Payment must reference a user account");
        }
    }

    public PaymentDto buildDto() {
        PaymentDto dto = new PaymentDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setAmount(getAmount());
        dto.setType(getType() != null ? getType().name() : null);
        dto.setPaymentDate(getPaymentDate());
        dto.setOrder(getOrder() != null ? new PaymentOrderDto(getOrder()) : null);
        dto.setUser(getUser() != null ? new PaymentUserDto(getUser()) : null);
        return dto;
    }
}