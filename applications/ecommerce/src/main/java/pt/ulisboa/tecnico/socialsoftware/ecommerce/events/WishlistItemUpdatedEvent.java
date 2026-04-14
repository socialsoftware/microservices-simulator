package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class WishlistItemUpdatedEvent extends Event {
    @Column(name = "wishlist_item_updated_event_added_at")
    private String addedAt;

    public WishlistItemUpdatedEvent() {
        super();
    }

    public WishlistItemUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public WishlistItemUpdatedEvent(Integer aggregateId, String addedAt) {
        super(aggregateId);
        setAddedAt(addedAt);
    }

    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }

}