package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;

@Entity
public class SagaWishlistItem extends WishlistItem implements SagaAggregate {
    private SagaState sagaState;

    public SagaWishlistItem() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaWishlistItem(SagaWishlistItem other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaWishlistItem(Integer aggregateId, WishlistItemDto wishlistitemDto) {
        super(aggregateId, wishlistitemDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}