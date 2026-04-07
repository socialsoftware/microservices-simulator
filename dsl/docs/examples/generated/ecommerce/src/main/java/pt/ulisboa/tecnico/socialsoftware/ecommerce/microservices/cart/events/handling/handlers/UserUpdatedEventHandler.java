package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.eventProcessing.CartEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;

public class UserUpdatedEventHandler extends CartEventHandler {
    public UserUpdatedEventHandler(CartRepository cartRepository, CartEventProcessing cartEventProcessing) {
        super(cartRepository, cartEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.cartEventProcessing.processUserUpdatedEvent(subscriberAggregateId, (UserUpdatedEvent) event);
    }
}
