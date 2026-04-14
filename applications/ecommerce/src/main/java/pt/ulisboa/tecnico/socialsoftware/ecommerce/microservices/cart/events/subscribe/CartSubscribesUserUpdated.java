package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartUser;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;

public class CartSubscribesUserUpdated extends EventSubscription {
    

    public CartSubscribesUserUpdated(CartUser cartUser) {
        super(cartUser.getUserAggregateId(),
                cartUser.getUserVersion(),
                UserUpdatedEvent.class.getSimpleName());
        
    }

    public CartSubscribesUserUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
