package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DiscountDeletedEvent extends Event {

    public DiscountDeletedEvent() {
        super();
    }

    public DiscountDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}