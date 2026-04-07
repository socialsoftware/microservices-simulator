package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderDeletedEvent;

public class ShippingSubscribesOrderDeleted extends EventSubscription {
    public ShippingSubscribesOrderDeleted(Shipping shipping) {
        super(shipping.getAggregateId(), 0, OrderDeletedEvent.class.getSimpleName());
    }
}
