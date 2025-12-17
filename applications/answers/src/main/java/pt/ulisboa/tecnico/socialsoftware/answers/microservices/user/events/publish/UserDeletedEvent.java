package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UserDeletedEvent extends Event {

    public UserDeletedEvent() {
    }

    public UserDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

}