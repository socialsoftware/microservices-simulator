package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.eventProcessing.RouteEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteRepository;

public abstract class RouteEventHandler extends EventHandler {
    private RouteRepository routeRepository;
    protected RouteEventProcessing routeEventProcessing;

    public RouteEventHandler(RouteRepository routeRepository, RouteEventProcessing routeEventProcessing) {
        this.routeRepository = routeRepository;
        this.routeEventProcessing = routeEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return routeRepository.findAll().stream().map(Route::getAggregateId).collect(Collectors.toSet());
    }

}
