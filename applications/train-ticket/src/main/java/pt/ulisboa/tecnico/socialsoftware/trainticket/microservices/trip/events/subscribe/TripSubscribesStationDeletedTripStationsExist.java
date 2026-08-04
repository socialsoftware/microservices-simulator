package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripStartStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;


public class TripSubscribesStationDeletedTripStationsExist extends EventSubscription {
    public TripSubscribesStationDeletedTripStationsExist(TripStartStation startStation) {
        super(startStation.getStartStationAggregateId(),
                startStation.getStartStationVersion(),
                StationDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
