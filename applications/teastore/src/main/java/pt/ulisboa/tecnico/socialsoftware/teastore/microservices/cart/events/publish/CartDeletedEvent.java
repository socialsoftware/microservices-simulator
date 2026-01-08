package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CartDeletedEvent extends Event {

    public CartDeletedEvent() {
    }

    public CartDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

}