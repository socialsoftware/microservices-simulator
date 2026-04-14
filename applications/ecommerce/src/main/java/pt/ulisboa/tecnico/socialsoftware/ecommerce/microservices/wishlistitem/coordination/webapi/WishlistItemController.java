package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.functionalities.WishlistItemFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.webapi.requestDtos.CreateWishlistItemRequestDto;

@RestController
public class WishlistItemController {
    @Autowired
    private WishlistItemFunctionalities wishlistItemFunctionalities;

    @PostMapping("/wishlistitems/create")
    @ResponseStatus(HttpStatus.CREATED)
    public WishlistItemDto createWishlistItem(@RequestBody CreateWishlistItemRequestDto createRequest) {
        return wishlistItemFunctionalities.createWishlistItem(createRequest);
    }

    @GetMapping("/wishlistitems/{wishlistitemAggregateId}")
    public WishlistItemDto getWishlistItemById(@PathVariable Integer wishlistitemAggregateId) {
        return wishlistItemFunctionalities.getWishlistItemById(wishlistitemAggregateId);
    }

    @PutMapping("/wishlistitems")
    public WishlistItemDto updateWishlistItem(@RequestBody WishlistItemDto wishlistitemDto) {
        return wishlistItemFunctionalities.updateWishlistItem(wishlistitemDto);
    }

    @DeleteMapping("/wishlistitems/{wishlistitemAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWishlistItem(@PathVariable Integer wishlistitemAggregateId) {
        wishlistItemFunctionalities.deleteWishlistItem(wishlistitemAggregateId);
    }

    @GetMapping("/wishlistitems")
    public List<WishlistItemDto> getAllWishlistItems() {
        return wishlistItemFunctionalities.getAllWishlistItems();
    }
}
