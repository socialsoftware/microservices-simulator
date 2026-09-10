package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

public interface RouteFactory {
    Route createRoute(Integer aggregateId, RouteDto routeDto);

    Route createRouteCopy(Route existing);

    RouteDto createRouteDto(Route route);
}
