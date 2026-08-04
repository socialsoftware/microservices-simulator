package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;

public class TripSubscribesStationDeleted extends EventSubscription {
    public TripSubscribesStationDeleted(Trip trip) {
        super(trip.getAggregateId(), 0, StationDeletedEvent.class.getSimpleName());
    }
}
