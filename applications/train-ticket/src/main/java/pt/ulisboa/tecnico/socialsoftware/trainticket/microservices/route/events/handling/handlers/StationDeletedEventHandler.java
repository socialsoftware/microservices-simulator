package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.eventProcessing.RouteEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.StationDeletedEvent;

public class StationDeletedEventHandler extends RouteEventHandler {
    public StationDeletedEventHandler(RouteRepository routeRepository, RouteEventProcessing routeEventProcessing) {
        super(routeRepository, routeEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.routeEventProcessing.processStationDeletedEvent(subscriberAggregateId, (StationDeletedEvent) event);
    }
}
