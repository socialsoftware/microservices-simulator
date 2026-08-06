package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class TripRouteUpdatedEvent extends Event {
    private Integer routeAggregateId;
    private Long routeVersion;

    public TripRouteUpdatedEvent() {
        super();
    }

    public TripRouteUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TripRouteUpdatedEvent(Integer aggregateId, Integer routeAggregateId, Long routeVersion) {
        super(aggregateId);
        setRouteAggregateId(routeAggregateId);
        setRouteVersion(routeVersion);
    }

    public Integer getRouteAggregateId() {
        return routeAggregateId;
    }

    public void setRouteAggregateId(Integer routeAggregateId) {
        this.routeAggregateId = routeAggregateId;
    }

    public Long getRouteVersion() {
        return routeVersion;
    }

    public void setRouteVersion(Long routeVersion) {
        this.routeVersion = routeVersion;
    }

}
