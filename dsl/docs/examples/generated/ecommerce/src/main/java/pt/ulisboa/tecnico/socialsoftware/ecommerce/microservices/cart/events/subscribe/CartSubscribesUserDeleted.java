package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;

public class CartSubscribesUserDeleted extends EventSubscription {
    public CartSubscribesUserDeleted(Cart cart) {
        super(cart.getAggregateId(), 0, UserDeletedEvent.class.getSimpleName());
    }
}
