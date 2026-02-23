package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TopicCourseUpdatedEvent extends Event {
    private Integer courseAggregateId;
    private Integer courseVersion;

    public TopicCourseUpdatedEvent() {
        super();
    }

    public TopicCourseUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TopicCourseUpdatedEvent(Integer aggregateId, Integer courseAggregateId, Integer courseVersion) {
        super(aggregateId);
        setCourseAggregateId(courseAggregateId);
        setCourseVersion(courseVersion);
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseVersion() {
        return courseVersion;
    }

    public void setCourseVersion(Integer courseVersion) {
        this.courseVersion = courseVersion;
    }

}