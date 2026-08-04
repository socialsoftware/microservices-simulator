package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;

public class TripSubscribesRouteDeleted extends EventSubscription {
    public TripSubscribesRouteDeleted(Trip trip) {
        super(trip.getAggregateId(), 0, RouteDeletedEvent.class.getSimpleName());
    }
}
