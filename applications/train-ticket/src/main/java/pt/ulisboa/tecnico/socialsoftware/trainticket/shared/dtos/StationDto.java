package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;

public class StationDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;
    private String name;
    private Integer stayTime;

    public StationDto() {
    }

    public StationDto(Station station) {
        this.aggregateId = station.getAggregateId();
        this.version = station.getVersion();
        this.state = station.getState();
        this.name = station.getName();
        this.stayTime = station.getStayTime();
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

    public Integer getStayTime() {
        return stayTime;
    }

    public void setStayTime(Integer stayTime) {
        this.stayTime = stayTime;
    }
}