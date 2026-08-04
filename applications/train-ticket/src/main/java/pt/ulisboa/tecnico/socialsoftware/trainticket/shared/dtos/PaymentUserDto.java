package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.PaymentUser;

public class PaymentUserDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String state;

    public PaymentUserDto() {
    }

    public PaymentUserDto(PaymentUser paymentUser) {
        this.aggregateId = paymentUser.getUserAggregateId();
        this.version = paymentUser.getUserVersion();
        this.state = paymentUser.getUserState() != null ? paymentUser.getUserState().name() : null;
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