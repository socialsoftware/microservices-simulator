package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class TripTerminalStationUpdatedEvent extends Event {
    private Integer stationAggregateId;
    private Long stationVersion;
    private String terminalStationName;

    public TripTerminalStationUpdatedEvent() {
        super();
    }

    public TripTerminalStationUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TripTerminalStationUpdatedEvent(Integer aggregateId, Integer stationAggregateId, Long stationVersion, String terminalStationName) {
        super(aggregateId);
        setStationAggregateId(stationAggregateId);
        setStationVersion(stationVersion);
        setTerminalStationName(terminalStationName);
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

    public String getTerminalStationName() {
        return terminalStationName;
    }

    public void setTerminalStationName(String terminalStationName) {
        this.terminalStationName = terminalStationName;
    }

}
