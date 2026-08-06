package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;

public class RouteSubscribesStationDeleted extends EventSubscription {
    public RouteSubscribesStationDeleted(Route route) {
        super(route.getAggregateId(), 0L, StationDeletedEvent.class.getSimpleName());
    }
}
