package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;

public interface ShippingFactory {
    Shipping createShipping(Integer aggregateId, ShippingDto shippingDto);
    Shipping createShippingFromExisting(Shipping existingShipping);
    ShippingDto createShippingDto(Shipping shipping);
}
