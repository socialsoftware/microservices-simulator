package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.SagaPriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.dtos.SagaPriceConfigDto;

@Service
@Profile("sagas")
public class SagasPriceConfigFactory implements PriceConfigFactory {
    @Override
    public PriceConfig createPriceConfig(Integer aggregateId, PriceConfigDto priceconfigDto) {
        return new SagaPriceConfig(aggregateId, priceconfigDto);
    }

    @Override
    public PriceConfig createPriceConfigFromExisting(PriceConfig existingPriceConfig) {
        return new SagaPriceConfig((SagaPriceConfig) existingPriceConfig);
    }

    @Override
    public PriceConfigDto createPriceConfigDto(PriceConfig priceconfig) {
        return new SagaPriceConfigDto(priceconfig);
    }
}