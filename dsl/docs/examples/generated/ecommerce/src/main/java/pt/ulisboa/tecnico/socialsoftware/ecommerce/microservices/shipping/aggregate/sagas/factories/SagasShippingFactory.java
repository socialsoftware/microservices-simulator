package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.ShippingFactory;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.sagas.SagaShipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.sagas.dtos.SagaShippingDto;

@Service
@Profile("sagas")
public class SagasShippingFactory implements ShippingFactory {
    @Override
    public Shipping createShipping(Integer aggregateId, ShippingDto shippingDto) {
        return new SagaShipping(aggregateId, shippingDto);
    }

    @Override
    public Shipping createShippingFromExisting(Shipping existingShipping) {
        return new SagaShipping((SagaShipping) existingShipping);
    }

    @Override
    public ShippingDto createShippingDto(Shipping shipping) {
        return new SagaShippingDto(shipping);
    }
}