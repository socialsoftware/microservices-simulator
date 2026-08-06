package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderTrain;

public class OrderTrainDto implements Serializable {
    private String name;
    private Integer aggregateId;
    private Long version;
    private String state;

    public OrderTrainDto() {
    }

    public OrderTrainDto(OrderTrain orderTrain) {
        this.name = orderTrain.getTrainNumber();
        this.aggregateId = orderTrain.getTrainAggregateId();
        this.version = orderTrain.getTrainVersion();
        this.state = orderTrain.getTrainState() != null ? orderTrain.getTrainState().name() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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