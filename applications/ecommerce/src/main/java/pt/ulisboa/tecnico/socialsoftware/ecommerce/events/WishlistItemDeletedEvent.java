package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class WishlistItemDeletedEvent extends Event {

    public WishlistItemDeletedEvent() {
        super();
    }

    public WishlistItemDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}