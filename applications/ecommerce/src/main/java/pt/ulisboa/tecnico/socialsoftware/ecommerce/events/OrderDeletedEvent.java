package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderDeletedEvent extends Event {

    public OrderDeletedEvent() {
        super();
    }

    public OrderDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}