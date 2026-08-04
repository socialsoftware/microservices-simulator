package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class OrderToStationUpdatedEvent extends Event {
    private Integer stationAggregateId;
    private Integer stationVersion;
    private String toName;

    public OrderToStationUpdatedEvent() {
        super();
    }

    public OrderToStationUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderToStationUpdatedEvent(Integer aggregateId, Integer stationAggregateId, Integer stationVersion, String toName) {
        super(aggregateId);
        setStationAggregateId(stationAggregateId);
        setStationVersion(stationVersion);
        setToName(toName);
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

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

}
