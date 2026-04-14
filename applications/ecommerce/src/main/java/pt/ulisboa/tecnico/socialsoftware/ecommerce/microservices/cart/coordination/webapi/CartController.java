package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.functionalities.CartFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartItemDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.webapi.requestDtos.CreateCartRequestDto;

@RestController
public class CartController {
    @Autowired
    private CartFunctionalities cartFunctionalities;

    @PostMapping("/carts/create")
    @ResponseStatus(HttpStatus.CREATED)
    public CartDto createCart(@RequestBody CreateCartRequestDto createRequest) {
        return cartFunctionalities.createCart(createRequest);
    }

    @GetMapping("/carts/{cartAggregateId}")
    public CartDto getCartById(@PathVariable Integer cartAggregateId) {
        return cartFunctionalities.getCartById(cartAggregateId);
    }

    @PutMapping("/carts")
    public CartDto updateCart(@RequestBody CartDto cartDto) {
        return cartFunctionalities.updateCart(cartDto);
    }

    @DeleteMapping("/carts/{cartAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCart(@PathVariable Integer cartAggregateId) {
        cartFunctionalities.deleteCart(cartAggregateId);
    }

    @GetMapping("/carts")
    public List<CartDto> getAllCarts() {
        return cartFunctionalities.getAllCarts();
    }

    @PostMapping("/carts/{cartId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemDto addCartItem(@PathVariable Integer cartId, @RequestParam Integer quantity, @RequestBody CartItemDto itemDto) {
        return cartFunctionalities.addCartItem(cartId, quantity, itemDto);
    }

    @PostMapping("/carts/{cartId}/items/batch")
    public List<CartItemDto> addCartItems(@PathVariable Integer cartId, @RequestBody List<CartItemDto> itemDtos) {
        return cartFunctionalities.addCartItems(cartId, itemDtos);
    }

    @GetMapping("/carts/{cartId}/items/{quantity}")
    public CartItemDto getCartItem(@PathVariable Integer cartId, @PathVariable Integer quantity) {
        return cartFunctionalities.getCartItem(cartId, quantity);
    }

    @PutMapping("/carts/{cartId}/items/{quantity}")
    public CartItemDto updateCartItem(@PathVariable Integer cartId, @PathVariable Integer quantity, @RequestBody CartItemDto itemDto) {
        return cartFunctionalities.updateCartItem(cartId, quantity, itemDto);
    }

    @DeleteMapping("/carts/{cartId}/items/{quantity}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCartItem(@PathVariable Integer cartId, @PathVariable Integer quantity) {
        cartFunctionalities.removeCartItem(cartId, quantity);
    }
}
