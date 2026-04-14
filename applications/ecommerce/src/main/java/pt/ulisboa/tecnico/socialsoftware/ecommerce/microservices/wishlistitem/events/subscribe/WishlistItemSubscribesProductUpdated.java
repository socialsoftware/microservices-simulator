package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemProduct;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ProductUpdatedEvent;

public class WishlistItemSubscribesProductUpdated extends EventSubscription {
    

    public WishlistItemSubscribesProductUpdated(WishlistItemProduct wishlistItemProduct) {
        super(wishlistItemProduct.getProductAggregateId(),
                wishlistItemProduct.getProductVersion(),
                ProductUpdatedEvent.class.getSimpleName());
        
    }

    public WishlistItemSubscribesProductUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
