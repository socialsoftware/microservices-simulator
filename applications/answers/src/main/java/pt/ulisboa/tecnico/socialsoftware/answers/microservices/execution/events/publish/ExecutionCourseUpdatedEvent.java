package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ExecutionCourseUpdatedEvent extends Event {
    private Integer courseAggregateId;
    private Integer courseVersion;
    private String courseName;

    public ExecutionCourseUpdatedEvent() {
        super();
    }

    public ExecutionCourseUpdatedEvent(Integer aggregateId, Integer courseAggregateId, Integer courseVersion, String courseName) {
        super(aggregateId);
        setCourseAggregateId(courseAggregateId);
        setCourseVersion(courseVersion);
        setCourseName(courseName);
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

}