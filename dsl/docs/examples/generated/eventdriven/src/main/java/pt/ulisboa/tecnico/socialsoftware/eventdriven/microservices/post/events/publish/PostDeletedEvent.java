package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class PostDeletedEvent extends Event {

    public PostDeletedEvent() {
        super();
    }

    public PostDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}