package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.eventProcessing.TripEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.TrainDeletedEvent;

public class TrainDeletedEventHandler extends TripEventHandler {
    public TrainDeletedEventHandler(TripRepository tripRepository, TripEventProcessing tripEventProcessing) {
        super(tripRepository, tripEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.tripEventProcessing.processTrainDeletedEvent(subscriberAggregateId, (TrainDeletedEvent) event);
    }
}
