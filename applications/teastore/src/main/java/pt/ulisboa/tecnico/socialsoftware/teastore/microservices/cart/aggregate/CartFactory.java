package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;

public interface CartFactory {
    Cart createCart(Integer aggregateId, CartDto cartDto);
    Cart createCartFromExisting(Cart existingCart);
    CartDto createCartDto(Cart cart);
}
