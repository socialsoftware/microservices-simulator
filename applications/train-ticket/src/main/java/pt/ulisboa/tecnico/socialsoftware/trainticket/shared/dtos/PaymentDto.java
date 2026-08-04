package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.Payment;

public class PaymentDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private Double amount;
    private String type;
    private String paymentDate;
    private PaymentOrderDto order;
    private PaymentUserDto user;

    public PaymentDto() {
    }

    public PaymentDto(Payment payment) {
        this.aggregateId = payment.getAggregateId();
        this.version = payment.getVersion();
        this.state = payment.getState();
        this.amount = payment.getAmount();
        this.type = payment.getType() != null ? payment.getType().name() : null;
        this.paymentDate = payment.getPaymentDate();
        this.order = payment.getOrder() != null ? new PaymentOrderDto(payment.getOrder()) : null;
        this.user = payment.getUser() != null ? new PaymentUserDto(payment.getUser()) : null;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentOrderDto getOrder() {
        return order;
    }

    public void setOrder(PaymentOrderDto order) {
        this.order = order;
    }

    public PaymentUserDto getUser() {
        return user;
    }

    public void setUser(PaymentUserDto user) {
        this.user = user;
    }
}