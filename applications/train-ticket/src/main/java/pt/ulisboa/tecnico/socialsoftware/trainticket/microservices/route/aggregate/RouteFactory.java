package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;

public interface RouteFactory {
    Route createRoute(Integer aggregateId, RouteDto routeDto);
    Route createRouteFromExisting(Route existingRoute);
    RouteDto createRouteDto(Route route);
}
