package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.functionalities.CartFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.*;

@RestController
public class CartController {
    @Autowired
    private CartFunctionalities cartFunctionalities;

    @PostMapping("/carts/create")
    public CartDto createCart(@RequestBody CartDto cartDto) throws Exception {
        return cartFunctionalities.createCart(cartDto);
    }

    @PostMapping("/carts/{cartAggregateId}/add")
    public CartDto addItem(@PathVariable Long cartAggregateId, @RequestParam Long productId, @RequestParam String productName, @RequestParam Double unitPriceInCents, @RequestParam Integer quantity) throws Exception {
        return cartFunctionalities.addItem(cartAggregateId, productId, productName, unitPriceInCents, quantity);
    }

    @PutMapping("/carts/{cartAggregateId}/update")
    public CartDto updateItem(@PathVariable Long cartAggregateId, @RequestParam Long productId, @RequestParam Integer quantity) throws Exception {
        return cartFunctionalities.updateItem(cartAggregateId, productId, quantity);
    }

    @DeleteMapping("/carts/{cartAggregateId}/remove")
    public CartDto removeItem(@PathVariable Long cartAggregateId, @RequestParam Long productId) throws Exception {
        return cartFunctionalities.removeItem(cartAggregateId, productId);
    }

    @PostMapping("/carts/{cartAggregateId}/checkout")
    public CartDto checkoutCart(@PathVariable Long cartAggregateId) throws Exception {
        return cartFunctionalities.checkoutCart(cartAggregateId);
    }

    @GetMapping("/carts/user/{userId}")
    public CartDto findByUserId(@PathVariable Long userId) {
        return cartFunctionalities.findByUserId(userId);
    }
}
