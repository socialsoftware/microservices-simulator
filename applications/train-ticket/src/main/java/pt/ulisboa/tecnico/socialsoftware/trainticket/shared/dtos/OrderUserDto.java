package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderUser;

public class OrderUserDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String state;

    public OrderUserDto() {
    }

    public OrderUserDto(OrderUser orderUser) {
        this.aggregateId = orderUser.getUserAggregateId();
        this.version = orderUser.getUserVersion();
        this.state = orderUser.getUserState() != null ? orderUser.getUserState().name() : null;
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