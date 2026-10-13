package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import java.util.List;

public interface RouteFactory {
    Route createRoute(Integer aggregateId, List<RouteStation> stations);

    Route createRouteCopy(Route existing);

    RouteDto createRouteDto(Route route);
}
