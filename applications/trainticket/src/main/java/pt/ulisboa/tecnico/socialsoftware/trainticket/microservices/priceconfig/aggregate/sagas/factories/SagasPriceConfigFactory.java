package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.SagaPriceConfig;

@Service
@Profile("sagas")
public class SagasPriceConfigFactory implements PriceConfigFactory {
    @Override
    public SagaPriceConfig createPriceConfig(Integer aggregateId, PriceConfigDto priceConfigDto) {
        return new SagaPriceConfig(aggregateId, priceConfigDto);
    }

    @Override
    public SagaPriceConfig createPriceConfigCopy(PriceConfig existing) {
        return new SagaPriceConfig((SagaPriceConfig) existing);
    }

    @Override
    public PriceConfigDto createPriceConfigDto(PriceConfig priceConfig) {
        return new PriceConfigDto(priceConfig);
    }
}
