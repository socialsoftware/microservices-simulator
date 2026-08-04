package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class ContactsDeletedEvent extends Event {

    public ContactsDeletedEvent() {
        super();
    }

    public ContactsDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
