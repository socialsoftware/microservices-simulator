package pt.ulisboa.tecnico.socialsoftware.tutorial.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class LoanDeletedEvent extends Event {

    public LoanDeletedEvent() {
        super();
    }

    public LoanDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}