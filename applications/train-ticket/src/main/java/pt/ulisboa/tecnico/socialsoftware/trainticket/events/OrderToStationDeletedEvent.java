package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class OrderToStationDeletedEvent extends Event {
    private Integer stationAggregateId;

    public OrderToStationDeletedEvent() {
        super();
    }

    public OrderToStationDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderToStationDeletedEvent(Integer aggregateId, Integer stationAggregateId) {
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
