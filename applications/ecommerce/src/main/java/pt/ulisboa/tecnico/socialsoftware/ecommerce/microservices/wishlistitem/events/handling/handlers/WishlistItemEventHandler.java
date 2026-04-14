package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.eventProcessing.WishlistItemEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemRepository;

public abstract class WishlistItemEventHandler extends EventHandler {
    private WishlistItemRepository wishlistitemRepository;
    protected WishlistItemEventProcessing wishlistitemEventProcessing;

    public WishlistItemEventHandler(WishlistItemRepository wishlistitemRepository, WishlistItemEventProcessing wishlistitemEventProcessing) {
        this.wishlistitemRepository = wishlistitemRepository;
        this.wishlistitemEventProcessing = wishlistitemEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return wishlistitemRepository.findAll().stream().map(WishlistItem::getAggregateId).collect(Collectors.toSet());
    }

}
