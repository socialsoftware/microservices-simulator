package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigRepository;

import java.util.List;
import java.util.Optional;

@Service
@Profile("sagas")
public class PriceConfigCustomRepositorySagas implements PriceConfigCustomRepository {
    @Autowired
    private PriceConfigRepository priceConfigRepository;

    @Override
    public List<PriceConfig> findAllLatestActive() {
        return priceConfigRepository.findAllLatestActive();
    }

    @Override
    public Optional<PriceConfig> findLatestActiveByRouteAndTrainType(Integer routeAggregateId,
                                                                    Integer trainTypeAggregateId) {
        return priceConfigRepository.findLatestActiveByRouteAndTrainType(routeAggregateId, trainTypeAggregateId);
    }
}
