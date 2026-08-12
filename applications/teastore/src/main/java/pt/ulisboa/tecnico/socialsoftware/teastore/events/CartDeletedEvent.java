package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CartDeletedEvent extends Event {

    public CartDeletedEvent() {
        super();
    }

    public CartDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}