package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.SagaRoute;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.dtos.SagaRouteDto;

@Service
@Profile("sagas")
public class SagasRouteFactory implements RouteFactory {
    @Override
    public Route createRoute(Integer aggregateId, RouteDto routeDto) {
        return new SagaRoute(aggregateId, routeDto);
    }

    @Override
    public Route createRouteFromExisting(Route existingRoute) {
        return new SagaRoute((SagaRoute) existingRoute);
    }

    @Override
    public RouteDto createRouteDto(Route route) {
        return new SagaRouteDto(route);
    }
}