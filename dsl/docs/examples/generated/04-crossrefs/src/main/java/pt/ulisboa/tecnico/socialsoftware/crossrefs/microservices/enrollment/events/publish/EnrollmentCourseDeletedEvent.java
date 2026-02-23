package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class EnrollmentCourseDeletedEvent extends Event {
    private Integer courseAggregateId;

    public EnrollmentCourseDeletedEvent() {
        super();
    }

    public EnrollmentCourseDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public EnrollmentCourseDeletedEvent(Integer aggregateId, Integer courseAggregateId) {
        super(aggregateId);
        setCourseAggregateId(courseAggregateId);
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

}