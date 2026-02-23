package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.events.publish;

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