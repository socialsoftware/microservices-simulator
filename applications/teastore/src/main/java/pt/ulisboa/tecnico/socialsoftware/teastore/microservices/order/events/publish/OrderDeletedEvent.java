package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OrderDeletedEvent extends Event {

    public OrderDeletedEvent() {
    }

    public OrderDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

}