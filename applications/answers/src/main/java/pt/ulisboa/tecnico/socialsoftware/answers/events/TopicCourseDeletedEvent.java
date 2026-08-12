package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TopicCourseDeletedEvent extends Event {
    private Integer courseAggregateId;

    public TopicCourseDeletedEvent() {
        super();
    }

    public TopicCourseDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TopicCourseDeletedEvent(Integer aggregateId, Integer courseAggregateId) {
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