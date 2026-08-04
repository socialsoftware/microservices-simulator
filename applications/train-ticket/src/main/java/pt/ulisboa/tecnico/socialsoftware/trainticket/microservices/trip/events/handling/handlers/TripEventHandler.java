package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.eventProcessing.TripEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripRepository;

public abstract class TripEventHandler extends EventHandler {
    private TripRepository tripRepository;
    protected TripEventProcessing tripEventProcessing;

    public TripEventHandler(TripRepository tripRepository, TripEventProcessing tripEventProcessing) {
        this.tripRepository = tripRepository;
        this.tripEventProcessing = tripEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return tripRepository.findAll().stream().map(Trip::getAggregateId).collect(Collectors.toSet());
    }

}
