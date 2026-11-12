package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

public interface PriceConfigFactory {
    PriceConfig createPriceConfig(Integer aggregateId, PriceConfigDto priceConfigDto);

    PriceConfig createPriceConfigCopy(PriceConfig existing);

    PriceConfigDto createPriceConfigDto(PriceConfig priceConfig);
}
