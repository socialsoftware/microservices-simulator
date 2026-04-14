package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class WishlistItemProductDeletedEvent extends Event {
    @Column(name = "wishlist_item_product_deleted_event_product_aggregate_id")
    private Integer productAggregateId;

    public WishlistItemProductDeletedEvent() {
        super();
    }

    public WishlistItemProductDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public WishlistItemProductDeletedEvent(Integer aggregateId, Integer productAggregateId) {
        super(aggregateId);
        setProductAggregateId(productAggregateId);
    }

    public Integer getProductAggregateId() {
        return productAggregateId;
    }

    public void setProductAggregateId(Integer productAggregateId) {
        this.productAggregateId = productAggregateId;
    }

}