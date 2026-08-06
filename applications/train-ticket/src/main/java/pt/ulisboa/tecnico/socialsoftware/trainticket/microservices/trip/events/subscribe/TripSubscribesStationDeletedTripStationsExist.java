package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripStartStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;


public class TripSubscribesStationDeletedTripStationsExist extends EventSubscription {
    public TripSubscribesStationDeletedTripStationsExist(TripStartStation startStation) {
        super(startStation.getStationAggregateId(),
                startStation.getStationVersion(),
                StationDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
