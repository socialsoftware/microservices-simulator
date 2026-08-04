package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;


public class RouteSubscribesStationDeletedRouteStationsExist extends EventSubscription {
    public RouteSubscribesStationDeletedRouteStationsExist(RouteStation stations) {
        super(stations.getStationAggregateId(),
                stations.getStationVersion(),
                StationDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
