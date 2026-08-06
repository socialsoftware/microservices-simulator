package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class RouteStationUpdatedEvent extends Event {
    private Integer stationAggregateId;
    private Long stationVersion;
    private String stationName;
    private Integer stationOrder;
    private Integer distanceFromStart;

    public RouteStationUpdatedEvent() {
        super();
    }

    public RouteStationUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public RouteStationUpdatedEvent(Integer aggregateId, Integer stationAggregateId, Long stationVersion, String stationName, Integer stationOrder, Integer distanceFromStart) {
        super(aggregateId);
        setStationAggregateId(stationAggregateId);
        setStationVersion(stationVersion);
        setStationName(stationName);
        setStationOrder(stationOrder);
        setDistanceFromStart(distanceFromStart);
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }

    public void setStationAggregateId(Integer stationAggregateId) {
        this.stationAggregateId = stationAggregateId;
    }

    public Long getStationVersion() {
        return stationVersion;
    }

    public void setStationVersion(Long stationVersion) {
        this.stationVersion = stationVersion;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Integer getStationOrder() {
        return stationOrder;
    }

    public void setStationOrder(Integer stationOrder) {
        this.stationOrder = stationOrder;
    }

    public Integer getDistanceFromStart() {
        return distanceFromStart;
    }

    public void setDistanceFromStart(Integer distanceFromStart) {
        this.distanceFromStart = distanceFromStart;
    }

}
