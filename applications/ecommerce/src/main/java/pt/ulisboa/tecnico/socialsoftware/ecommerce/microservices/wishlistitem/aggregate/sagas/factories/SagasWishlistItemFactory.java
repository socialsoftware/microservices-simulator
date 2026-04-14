package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemFactory;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.sagas.SagaWishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.sagas.dtos.SagaWishlistItemDto;

@Service
@Profile("sagas")
public class SagasWishlistItemFactory implements WishlistItemFactory {
    @Override
    public WishlistItem createWishlistItem(Integer aggregateId, WishlistItemDto wishlistitemDto) {
        return new SagaWishlistItem(aggregateId, wishlistitemDto);
    }

    @Override
    public WishlistItem createWishlistItemFromExisting(WishlistItem existingWishlistItem) {
        return new SagaWishlistItem((SagaWishlistItem) existingWishlistItem);
    }

    @Override
    public WishlistItemDto createWishlistItemDto(WishlistItem wishlistitem) {
        return new SagaWishlistItemDto(wishlistitem);
    }
}