package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
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
