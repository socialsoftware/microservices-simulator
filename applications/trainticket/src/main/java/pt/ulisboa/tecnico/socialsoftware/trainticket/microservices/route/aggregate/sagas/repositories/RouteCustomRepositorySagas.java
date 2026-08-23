package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteRepository;

import java.util.List;

@Service
@Profile("sagas")
public class RouteCustomRepositorySagas implements RouteCustomRepository {
    @Autowired
    private RouteRepository routeRepository;

    @Override
    public List<Route> findAllLatestActive() {
        return routeRepository.findAllLatestActive();
    }

    @Override
    public List<Route> findAllLatestActiveByStation(Integer stationAggregateId) {
        return routeRepository.findAllLatestActiveByStation(stationAggregateId);
    }
}
