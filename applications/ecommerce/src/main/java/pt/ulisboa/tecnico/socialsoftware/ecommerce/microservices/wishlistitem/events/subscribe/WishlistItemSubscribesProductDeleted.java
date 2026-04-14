package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ProductDeletedEvent;

public class WishlistItemSubscribesProductDeleted extends EventSubscription {
    public WishlistItemSubscribesProductDeleted(WishlistItem wishlistitem) {
        super(wishlistitem.getAggregateId(), 0, ProductDeletedEvent.class.getSimpleName());
    }
}
