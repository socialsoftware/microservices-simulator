package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class OrderTripDeletedEvent extends Event {
    private Integer tripAggregateId;

    public OrderTripDeletedEvent() {
        super();
    }

    public OrderTripDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OrderTripDeletedEvent(Integer aggregateId, Integer tripAggregateId) {
        super(aggregateId);
        setTripAggregateId(tripAggregateId);
    }

    public Integer getTripAggregateId() {
        return tripAggregateId;
    }

    public void setTripAggregateId(Integer tripAggregateId) {
        this.tripAggregateId = tripAggregateId;
    }

}
