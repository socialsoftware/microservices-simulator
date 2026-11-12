package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;

public class TrainTypeDto {
    private Integer aggregateId;
    private Long version;
    private Aggregate.AggregateState state;
    private String name;
    private Integer economyClassSeats;
    private Integer firstClassSeats;
    private Integer averageSpeed;

    public TrainTypeDto() {
    }

    public TrainTypeDto(String name, Integer economyClassSeats, Integer firstClassSeats, Integer averageSpeed) {
        this.name = name;
        this.economyClassSeats = economyClassSeats;
        this.firstClassSeats = firstClassSeats;
        this.averageSpeed = averageSpeed;
    }

    public TrainTypeDto(TrainType trainType) {
        this.aggregateId = trainType.getAggregateId();
        this.version = trainType.getVersion();
        this.state = trainType.getState();
        this.name = trainType.getName();
        this.economyClassSeats = trainType.getEconomyClassSeats();
        this.firstClassSeats = trainType.getFirstClassSeats();
        this.averageSpeed = trainType.getAverageSpeed();
    }

    public boolean isActive() {
        return this.state == Aggregate.AggregateState.ACTIVE;
    }

    public Integer getAggregateId() {
        return this.aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Aggregate.AggregateState getState() {
        return this.state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getEconomyClassSeats() {
        return this.economyClassSeats;
    }

    public void setEconomyClassSeats(Integer economyClassSeats) {
        this.economyClassSeats = economyClassSeats;
    }

    public Integer getFirstClassSeats() {
        return this.firstClassSeats;
    }

    public void setFirstClassSeats(Integer firstClassSeats) {
        this.firstClassSeats = firstClassSeats;
    }

    public Integer getAverageSpeed() {
        return this.averageSpeed;
    }

    public void setAverageSpeed(Integer averageSpeed) {
        this.averageSpeed = averageSpeed;
    }
}
