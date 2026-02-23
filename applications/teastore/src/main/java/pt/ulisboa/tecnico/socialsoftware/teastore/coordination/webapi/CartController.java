package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.functionalities.CartFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos.CreateCartRequestDto;

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
}
