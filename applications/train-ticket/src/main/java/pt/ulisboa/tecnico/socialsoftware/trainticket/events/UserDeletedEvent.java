package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class UserDeletedEvent extends Event {

    public UserDeletedEvent() {
        super();
    }

    public UserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}
