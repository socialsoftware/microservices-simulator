package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartDto;

public interface CartFactory {
    Cart createCart(Integer aggregateId, CartDto cartDto);
    Cart createCartFromExisting(Cart existingCart);
    CartDto createCartDto(Cart cart);
}
