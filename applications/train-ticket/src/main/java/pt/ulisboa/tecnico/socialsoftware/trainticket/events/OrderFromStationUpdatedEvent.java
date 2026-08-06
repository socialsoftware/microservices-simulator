package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class OrderFromStationUpdatedEvent extends Event {
    private Integer stationAggregateId;
    private Long stationVersion;
    private String fromName;

    public OrderFromStationUpdatedEvent() {
        super();
    }

    public OrderFromStationUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderFromStationUpdatedEvent(Integer aggregateId, Integer stationAggregateId, Long stationVersion, String fromName) {
        super(aggregateId);
        setStationAggregateId(stationAggregateId);
        setStationVersion(stationVersion);
        setFromName(fromName);
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

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

}
