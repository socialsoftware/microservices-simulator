package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;

public class WishlistItemSubscribesUserDeleted extends EventSubscription {
    public WishlistItemSubscribesUserDeleted(WishlistItem wishlistitem) {
        super(wishlistitem.getAggregateId(), 0, UserDeletedEvent.class.getSimpleName());
    }
}
