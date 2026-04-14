package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UserDeletedEvent extends Event {

    public UserDeletedEvent() {
        super();
    }

    public UserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}