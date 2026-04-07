package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.eventProcessing.CartEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.handling.handlers.UserUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;

@Component
public class CartEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private CartEventProcessing cartEventProcessing;
    @Autowired
    private CartRepository cartRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleUserUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(UserUpdatedEvent.class,
                new UserUpdatedEventHandler(cartRepository, cartEventProcessing));
    }

}