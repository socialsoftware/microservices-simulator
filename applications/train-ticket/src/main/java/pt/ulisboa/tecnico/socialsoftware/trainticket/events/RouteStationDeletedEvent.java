package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class RouteStationDeletedEvent extends Event {
    private Integer stationAggregateId;

    public RouteStationDeletedEvent() {
        super();
    }

    public RouteStationDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public RouteStationDeletedEvent(Integer aggregateId, Integer stationAggregateId) {
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
