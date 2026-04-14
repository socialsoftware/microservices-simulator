package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.eventProcessing.WishlistItemEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ProductUpdatedEvent;

public class ProductUpdatedEventHandler extends WishlistItemEventHandler {
    public ProductUpdatedEventHandler(WishlistItemRepository wishlistitemRepository, WishlistItemEventProcessing wishlistitemEventProcessing) {
        super(wishlistitemRepository, wishlistitemEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.wishlistitemEventProcessing.processProductUpdatedEvent(subscriberAggregateId, (ProductUpdatedEvent) event);
    }
}
