package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;

public interface PriceConfigFactory {
    PriceConfig createPriceConfig(Integer aggregateId, PriceConfigDto priceconfigDto);
    PriceConfig createPriceConfigFromExisting(PriceConfig existingPriceConfig);
    PriceConfigDto createPriceConfigDto(PriceConfig priceconfig);
}
