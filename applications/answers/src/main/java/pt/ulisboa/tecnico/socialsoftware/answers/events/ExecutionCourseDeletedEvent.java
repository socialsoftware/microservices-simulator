package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ExecutionCourseDeletedEvent extends Event {
    @Column(name = "execution_course_deleted_event_course_aggregate_id")
    private Integer courseAggregateId;

    public ExecutionCourseDeletedEvent() {
        super();
    }

    public ExecutionCourseDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public ExecutionCourseDeletedEvent(Integer aggregateId, Integer courseAggregateId) {
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