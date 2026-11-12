package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;

public class StationDto {
    private Integer aggregateId;
    private Long version;
    private Aggregate.AggregateState state;
    private String name;
    private Integer stayTime;

    public StationDto() {
    }

    public StationDto(String name, Integer stayTime) {
        this.name = name;
        this.stayTime = stayTime;
    }

    public StationDto(Station station) {
        this.aggregateId = station.getAggregateId();
        this.version = station.getVersion();
        this.state = station.getState();
        this.name = station.getName();
        this.stayTime = station.getStayTime();
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

    public Integer getStayTime() {
        return this.stayTime;
    }

    public void setStayTime(Integer stayTime) {
        this.stayTime = stayTime;
    }
}
