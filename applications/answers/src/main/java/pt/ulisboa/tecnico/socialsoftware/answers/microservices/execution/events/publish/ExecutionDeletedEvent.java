package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish;

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