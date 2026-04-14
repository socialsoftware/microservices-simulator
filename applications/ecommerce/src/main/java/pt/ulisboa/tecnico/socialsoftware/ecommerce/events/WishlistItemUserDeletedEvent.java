package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class WishlistItemUserDeletedEvent extends Event {
    @Column(name = "wishlist_item_user_deleted_event_user_aggregate_id")
    private Integer userAggregateId;

    public WishlistItemUserDeletedEvent() {
        super();
    }

    public WishlistItemUserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public WishlistItemUserDeletedEvent(Integer aggregateId, Integer userAggregateId) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

}