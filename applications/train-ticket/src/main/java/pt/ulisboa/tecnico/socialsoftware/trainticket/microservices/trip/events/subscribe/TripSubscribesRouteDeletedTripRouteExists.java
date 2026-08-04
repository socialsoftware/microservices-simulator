package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRoute;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;


public class TripSubscribesRouteDeletedTripRouteExists extends EventSubscription {
    public TripSubscribesRouteDeletedTripRouteExists(TripRoute route) {
        super(route.getRouteAggregateId(),
                route.getRouteVersion(),
                RouteDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
