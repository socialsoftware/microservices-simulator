package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;

public class PaymentDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private PaymentOrderDto order;
    private Double amountInCents;
    private String status;
    private String authorizationCode;
    private String paymentMethod;

    public PaymentDto() {
    }

    public PaymentDto(Payment payment) {
        this.aggregateId = payment.getAggregateId();
        this.version = payment.getVersion();
        this.state = payment.getState();
        this.order = payment.getOrder() != null ? new PaymentOrderDto(payment.getOrder()) : null;
        this.amountInCents = payment.getAmountInCents();
        this.status = payment.getStatus() != null ? payment.getStatus().name() : null;
        this.authorizationCode = payment.getAuthorizationCode();
        this.paymentMethod = payment.getPaymentMethod();
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

    public PaymentOrderDto getOrder() {
        return order;
    }

    public void setOrder(PaymentOrderDto order) {
        this.order = order;
    }

    public Double getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(Double amountInCents) {
        this.amountInCents = amountInCents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
}