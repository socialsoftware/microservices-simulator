package pt.ulisboa.tecnico.socialsoftware.crossrefs.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class EnrollmentCourseDeletedEvent extends Event {
    @Column(name = "enrollment_course_deleted_event_course_aggregate_id")
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