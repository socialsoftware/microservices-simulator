package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;

public class TripSubscribesRouteDeleted extends EventSubscription {
    public TripSubscribesRouteDeleted(Trip trip) {
        super(trip.getAggregateId(), 0L, RouteDeletedEvent.class.getSimpleName());
    }
}
