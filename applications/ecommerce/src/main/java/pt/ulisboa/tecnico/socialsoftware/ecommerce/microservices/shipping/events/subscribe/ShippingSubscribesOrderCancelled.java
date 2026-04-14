package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;

public class ShippingSubscribesOrderCancelled extends EventSubscription {
    

    public ShippingSubscribesOrderCancelled(Shipping shipping) {
        super(shipping.getAggregateId(),
                0,
                OrderCancelledEvent.class.getSimpleName());
        
    }

    public ShippingSubscribesOrderCancelled() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
