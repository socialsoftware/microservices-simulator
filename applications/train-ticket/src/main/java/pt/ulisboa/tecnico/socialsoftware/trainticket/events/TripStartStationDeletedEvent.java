package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class TripStartStationDeletedEvent extends Event {
    private Integer stationAggregateId;

    public TripStartStationDeletedEvent() {
        super();
    }

    public TripStartStationDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TripStartStationDeletedEvent(Integer aggregateId, Integer stationAggregateId) {
        super(aggregateId);
        setStationAggregateId(stationAggregateId);
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }

    public void setStationAggregateId(Integer stationAggregateId) {
        this.stationAggregateId = stationAggregateId;
    }

}
