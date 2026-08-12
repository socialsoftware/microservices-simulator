package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ExecutionDeletedEvent extends Event {

    public ExecutionDeletedEvent() {
        super();
    }

    public ExecutionDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}