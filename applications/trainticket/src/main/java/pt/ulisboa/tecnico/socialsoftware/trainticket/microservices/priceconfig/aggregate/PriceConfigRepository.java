package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

import org.springframework.data.jpa.repository.Query;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;
import java.util.Optional;

public interface PriceConfigRepository extends AggregateRepository {
    @Query("select p from PriceConfig p " +
            "where p.state = 'ACTIVE' " +
            "and p.version = (select max(p2.version) from PriceConfig p2 where p2.aggregateId = p.aggregateId)")
    List<PriceConfig> findAllLatestActive();

    @Query("select p from PriceConfig p " +
            "where p.state = 'ACTIVE' " +
            "and p.routeAggregateId = :routeAggregateId " +
            "and p.trainTypeAggregateId = :trainTypeAggregateId " +
            "and p.version = (select max(p2.version) from PriceConfig p2 where p2.aggregateId = p.aggregateId)")
    Optional<PriceConfig> findLatestActiveByRouteAndTrainType(Integer routeAggregateId, Integer trainTypeAggregateId);
}
