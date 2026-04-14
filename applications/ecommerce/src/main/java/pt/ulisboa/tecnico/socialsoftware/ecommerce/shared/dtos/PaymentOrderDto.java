package pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.PaymentOrder;

public class PaymentOrderDto implements Serializable {
    private Double totalInCents;
    private String status;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public PaymentOrderDto() {
    }

    public PaymentOrderDto(PaymentOrder paymentOrder) {
        this.totalInCents = paymentOrder.getOrderTotalInCents();
        this.status = paymentOrder.getOrderStatus() != null ? paymentOrder.getOrderStatus().name() : null;
        this.aggregateId = paymentOrder.getOrderAggregateId();
        this.version = paymentOrder.getOrderVersion();
        this.state = paymentOrder.getOrderState() != null ? paymentOrder.getOrderState().name() : null;
    }

    public Double getTotalInCents() {
        return totalInCents;
    }

    public void setTotalInCents(Double totalInCents) {
        this.totalInCents = totalInCents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}