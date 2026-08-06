package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.eventProcessing.TripEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRepository;

public abstract class TripEventHandler extends EventHandler {
    protected TripEventProcessing tripEventProcessing;

    public TripEventHandler(TripRepository tripRepository, TripEventProcessing tripEventProcessing) {
        super(tripRepository);
        this.tripEventProcessing = tripEventProcessing;
    }

}
