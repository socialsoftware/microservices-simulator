package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class TripStartStationUpdatedEvent extends Event {
    private Integer stationAggregateId;
    private Integer stationVersion;
    private String startStationName;

    public TripStartStationUpdatedEvent() {
        super();
    }

    public TripStartStationUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TripStartStationUpdatedEvent(Integer aggregateId, Integer stationAggregateId, Integer stationVersion, String startStationName) {
        super(aggregateId);
        setStationAggregateId(stationAggregateId);
        setStationVersion(stationVersion);
        setStartStationName(startStationName);
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }

    public void setStationAggregateId(Integer stationAggregateId) {
        this.stationAggregateId = stationAggregateId;
    }

    public Integer getStationVersion() {
        return stationVersion;
    }

    public void setStationVersion(Integer stationVersion) {
        this.stationVersion = stationVersion;
    }

    public String getStartStationName() {
        return startStationName;
    }

    public void setStartStationName(String startStationName) {
        this.startStationName = startStationName;
    }

}
