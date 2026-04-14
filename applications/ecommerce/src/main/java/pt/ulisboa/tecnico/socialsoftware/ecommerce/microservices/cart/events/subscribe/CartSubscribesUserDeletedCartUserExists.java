package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartUser;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;


public class CartSubscribesUserDeletedCartUserExists extends EventSubscription {
    public CartSubscribesUserDeletedCartUserExists(CartUser user) {
        super(user.getUserAggregateId(),
                user.getUserVersion(),
                UserDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
