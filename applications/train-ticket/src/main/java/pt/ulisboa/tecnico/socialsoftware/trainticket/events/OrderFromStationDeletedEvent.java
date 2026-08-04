package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class OrderFromStationDeletedEvent extends Event {
    private Integer stationAggregateId;

    public OrderFromStationDeletedEvent() {
        super();
    }

    public OrderFromStationDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderFromStationDeletedEvent(Integer aggregateId, Integer stationAggregateId) {
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
