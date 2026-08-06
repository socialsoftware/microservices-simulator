package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;

public class TripSubscribesTrainDeleted extends EventSubscription {
    public TripSubscribesTrainDeleted(Trip trip) {
        super(trip.getAggregateId(), 0L, TrainDeletedEvent.class.getSimpleName());
    }
}
