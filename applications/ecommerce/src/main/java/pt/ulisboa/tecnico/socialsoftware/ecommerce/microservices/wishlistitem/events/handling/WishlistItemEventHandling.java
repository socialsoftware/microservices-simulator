package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.eventProcessing.WishlistItemEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.handling.handlers.UserUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.handling.handlers.ProductUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ProductUpdatedEvent;

@Component
public class WishlistItemEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private WishlistItemEventProcessing wishlistitemEventProcessing;
    @Autowired
    private WishlistItemRepository wishlistitemRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleUserUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(UserUpdatedEvent.class,
                new UserUpdatedEventHandler(wishlistitemRepository, wishlistitemEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleProductUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ProductUpdatedEvent.class,
                new ProductUpdatedEventHandler(wishlistitemRepository, wishlistitemEventProcessing));
    }

}