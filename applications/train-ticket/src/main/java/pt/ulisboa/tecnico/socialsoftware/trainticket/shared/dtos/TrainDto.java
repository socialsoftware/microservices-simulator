package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.Train;

public class TrainDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;
    private String name;
    private Integer economyClass;
    private Integer confortClass;
    private Integer averageSpeed;

    public TrainDto() {
    }

    public TrainDto(Train train) {
        this.aggregateId = train.getAggregateId();
        this.version = train.getVersion();
        this.state = train.getState();
        this.name = train.getName();
        this.economyClass = train.getEconomyClass();
        this.confortClass = train.getConfortClass();
        this.averageSpeed = train.getAverageSpeed();
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getEconomyClass() {
        return economyClass;
    }

    public void setEconomyClass(Integer economyClass) {
        this.economyClass = economyClass;
    }

    public Integer getConfortClass() {
        return confortClass;
    }

    public void setConfortClass(Integer confortClass) {
        this.confortClass = confortClass;
    }

    public Integer getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(Integer averageSpeed) {
        this.averageSpeed = averageSpeed;
    }
}