package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.eventProcessing.ShippingEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.ShippingRepository;

public abstract class ShippingEventHandler extends EventHandler {
    private ShippingRepository shippingRepository;
    protected ShippingEventProcessing shippingEventProcessing;

    public ShippingEventHandler(ShippingRepository shippingRepository, ShippingEventProcessing shippingEventProcessing) {
        this.shippingRepository = shippingRepository;
        this.shippingEventProcessing = shippingEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return shippingRepository.findAll().stream().map(Shipping::getAggregateId).collect(Collectors.toSet());
    }

}
