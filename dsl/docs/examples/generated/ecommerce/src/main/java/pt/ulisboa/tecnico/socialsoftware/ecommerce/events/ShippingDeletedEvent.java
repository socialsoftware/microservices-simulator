package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ShippingDeletedEvent extends Event {

    public ShippingDeletedEvent() {
        super();
    }

    public ShippingDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}