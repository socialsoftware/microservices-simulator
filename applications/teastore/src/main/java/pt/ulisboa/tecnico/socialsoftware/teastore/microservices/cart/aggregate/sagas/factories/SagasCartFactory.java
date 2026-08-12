package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.CartFactory;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.sagas.SagaCart;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.sagas.dtos.SagaCartDto;

@Service
@Profile("sagas")
public class SagasCartFactory implements CartFactory {
    @Override
    public Cart createCart(Integer aggregateId, CartDto cartDto) {
        return new SagaCart(aggregateId, cartDto);
    }

    @Override
    public Cart createCartFromExisting(Cart existingCart) {
        return new SagaCart((SagaCart) existingCart);
    }

    @Override
    public CartDto createCartDto(Cart cart) {
        return new SagaCartDto(cart);
    }
}