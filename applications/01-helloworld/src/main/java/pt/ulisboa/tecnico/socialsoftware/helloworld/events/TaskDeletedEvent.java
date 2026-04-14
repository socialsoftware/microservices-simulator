package pt.ulisboa.tecnico.socialsoftware.helloworld.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TaskDeletedEvent extends Event {

    public TaskDeletedEvent() {
        super();
    }

    public TaskDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}