package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

public interface RouteRepository extends AggregateRepository {
    @Query("select r from Route r " +
            "where r.state = 'ACTIVE' " +
            "and r.version = (select max(r2.version) from Route r2 where r2.aggregateId = r.aggregateId)")
    List<Route> findAllLatestActive();

    @Query("select distinct r from Route r join r.routeStations rs " +
            "where rs.stationAggregateId = :stationAggregateId " +
            "and r.state = 'ACTIVE' " +
            "and r.version = (select max(r2.version) from Route r2 where r2.aggregateId = r.aggregateId)")
    List<Route> findAllLatestActiveByStation(@Param("stationAggregateId") Integer stationAggregateId);
}
