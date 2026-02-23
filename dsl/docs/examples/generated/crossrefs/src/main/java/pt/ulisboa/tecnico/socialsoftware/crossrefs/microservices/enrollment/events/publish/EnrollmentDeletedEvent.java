package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class EnrollmentDeletedEvent extends Event {

    public EnrollmentDeletedEvent() {
        super();
    }

    public EnrollmentDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}