package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStation;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.SagaRoute;

import java.util.List;

@Service
@Profile("sagas")
public class SagasRouteFactory implements RouteFactory {
    @Override
    public SagaRoute createRoute(Integer aggregateId, List<RouteStation> stations) {
        return new SagaRoute(aggregateId, stations);
    }

    @Override
    public SagaRoute createRouteCopy(Route existing) {
        return new SagaRoute((SagaRoute) existing);
    }

    @Override
    public RouteDto createRouteDto(Route route) {
        return new RouteDto(route);
    }
}
