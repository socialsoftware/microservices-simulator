package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CourseDeletedEvent extends Event {

    public CourseDeletedEvent() {
        super();
    }

    public CourseDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}