package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.eventProcessing.CartEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;

public class UserDeletedEventHandler extends CartEventHandler {
    public UserDeletedEventHandler(CartRepository cartRepository, CartEventProcessing cartEventProcessing) {
        super(cartRepository, cartEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.cartEventProcessing.processUserDeletedEvent(subscriberAggregateId, (UserDeletedEvent) event);
    }
}
