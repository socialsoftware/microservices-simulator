package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.PaymentOrder;

public class PaymentOrderDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private String state;

    public PaymentOrderDto() {
    }

    public PaymentOrderDto(PaymentOrder paymentOrder) {
        this.aggregateId = paymentOrder.getOrderAggregateId();
        this.version = paymentOrder.getOrderVersion();
        this.state = paymentOrder.getOrderState() != null ? paymentOrder.getOrderState().name() : null;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}