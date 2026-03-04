package pt.ulisboa.tecnico.socialsoftware.advanced.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CustomerDeletedEvent extends Event {

    public CustomerDeletedEvent() {
        super();
    }

    public CustomerDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}