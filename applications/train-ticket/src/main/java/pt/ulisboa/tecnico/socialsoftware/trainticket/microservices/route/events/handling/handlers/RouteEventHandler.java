package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.eventProcessing.RouteEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteRepository;

public abstract class RouteEventHandler extends EventHandler {
    protected RouteEventProcessing routeEventProcessing;

    public RouteEventHandler(RouteRepository routeRepository, RouteEventProcessing routeEventProcessing) {
        super(routeRepository);
        this.routeEventProcessing = routeEventProcessing;
    }

}
