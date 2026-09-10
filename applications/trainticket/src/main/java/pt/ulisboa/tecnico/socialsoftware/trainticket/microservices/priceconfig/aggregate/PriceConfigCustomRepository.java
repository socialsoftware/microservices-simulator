package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

import java.util.List;
import java.util.Optional;

public interface PriceConfigCustomRepository {
    List<PriceConfig> findAllLatestActive();

    Optional<PriceConfig> findLatestActiveByRouteAndTrainType(Integer routeAggregateId, Integer trainTypeAggregateId);
}
