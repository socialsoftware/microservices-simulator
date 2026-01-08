package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.CartFactory;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaCart;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.dtos.SagaCartDto;

@Service
@Profile("sagas")
public class SagasCartFactory extends CartFactory {
@Override
public Cart createCart(Integer aggregateId, CartDto cartDto) {
return new SagaCart(cartDto);
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