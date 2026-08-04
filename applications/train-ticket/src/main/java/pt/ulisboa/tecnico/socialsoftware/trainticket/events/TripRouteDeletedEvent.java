package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class TripRouteDeletedEvent extends Event {
    private Integer routeAggregateId;

    public TripRouteDeletedEvent() {
        super();
    }

    public TripRouteDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TripRouteDeletedEvent(Integer aggregateId, Integer routeAggregateId) {
        super(aggregateId);
        setRouteAggregateId(routeAggregateId);
    }

    public Integer getRouteAggregateId() {
        return routeAggregateId;
    }

    public void setRouteAggregateId(Integer routeAggregateId) {
        this.routeAggregateId = routeAggregateId;
    }

}
