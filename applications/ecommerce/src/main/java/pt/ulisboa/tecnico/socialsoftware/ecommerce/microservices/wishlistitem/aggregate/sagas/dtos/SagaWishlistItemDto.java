package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.sagas.SagaWishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaWishlistItemDto extends WishlistItemDto {
private SagaState sagaState;

public SagaWishlistItemDto(WishlistItem wishlistitem) {
super((WishlistItem) wishlistitem);
this.sagaState = ((SagaWishlistItem)wishlistitem).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}