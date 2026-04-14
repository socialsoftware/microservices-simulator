package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemUser;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;

public class WishlistItemSubscribesUserUpdated extends EventSubscription {
    

    public WishlistItemSubscribesUserUpdated(WishlistItemUser wishlistItemUser) {
        super(wishlistItemUser.getUserAggregateId(),
                wishlistItemUser.getUserVersion(),
                UserUpdatedEvent.class.getSimpleName());
        
    }

    public WishlistItemSubscribesUserUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
