package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.eventProcessing.TripEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.RouteDeletedEvent;

public class RouteDeletedEventHandler extends TripEventHandler {
    public RouteDeletedEventHandler(TripRepository tripRepository, TripEventProcessing tripEventProcessing) {
        super(tripRepository, tripEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.tripEventProcessing.processRouteDeletedEvent(subscriberAggregateId, (RouteDeletedEvent) event);
    }
}
