package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;

public interface WishlistItemFactory {
    WishlistItem createWishlistItem(Integer aggregateId, WishlistItemDto wishlistitemDto);
    WishlistItem createWishlistItemFromExisting(WishlistItem existingWishlistItem);
    WishlistItemDto createWishlistItemDto(WishlistItem wishlistitem);
}
