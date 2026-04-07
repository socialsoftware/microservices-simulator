package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.eventProcessing.CartEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartRepository;

public abstract class CartEventHandler extends EventHandler {
    private CartRepository cartRepository;
    protected CartEventProcessing cartEventProcessing;

    public CartEventHandler(CartRepository cartRepository, CartEventProcessing cartEventProcessing) {
        this.cartRepository = cartRepository;
        this.cartEventProcessing = cartEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return cartRepository.findAll().stream().map(Cart::getAggregateId).collect(Collectors.toSet());
    }

}
