package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.PaymentAuthorizedEvent;

public class ShippingSubscribesPaymentAuthorized extends EventSubscription {
    

    public ShippingSubscribesPaymentAuthorized(Shipping shipping) {
        super(shipping.getAggregateId(),
                0,
                PaymentAuthorizedEvent.class.getSimpleName());
        
    }

    public ShippingSubscribesPaymentAuthorized() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
