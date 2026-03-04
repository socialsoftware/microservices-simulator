package pt.ulisboa.tecnico.socialsoftware.typesenums.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ContactDeletedEvent extends Event {

    public ContactDeletedEvent() {
        super();
    }

    public ContactDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}