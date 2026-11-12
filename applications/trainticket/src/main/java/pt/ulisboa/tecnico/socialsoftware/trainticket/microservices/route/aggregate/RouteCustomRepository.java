package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import java.util.List;

public interface RouteCustomRepository {
    List<Route> findAllLatestActive();

    List<Route> findAllLatestActiveByStation(Integer stationAggregateId);
}
