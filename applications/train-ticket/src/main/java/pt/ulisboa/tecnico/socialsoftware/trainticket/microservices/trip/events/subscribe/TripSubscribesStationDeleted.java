package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;

public class TripSubscribesStationDeleted extends EventSubscription {
    public TripSubscribesStationDeleted(Trip trip) {
        super(trip.getAggregateId(), 0L, StationDeletedEvent.class.getSimpleName());
    }
}
