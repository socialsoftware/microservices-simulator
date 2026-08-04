package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class PriceConfigRouteUpdatedEvent extends Event {
    private Integer routeAggregateId;
    private Integer routeVersion;

    public PriceConfigRouteUpdatedEvent() {
        super();
    }

    public PriceConfigRouteUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PriceConfigRouteUpdatedEvent(Integer aggregateId, Integer routeAggregateId, Integer routeVersion) {
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

    public Integer getRouteVersion() {
        return routeVersion;
    }

    public void setRouteVersion(Integer routeVersion) {
        this.routeVersion = routeVersion;
    }

}
