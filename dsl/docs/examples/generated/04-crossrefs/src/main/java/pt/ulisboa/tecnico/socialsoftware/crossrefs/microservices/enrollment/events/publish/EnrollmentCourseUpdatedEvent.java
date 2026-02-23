package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class EnrollmentCourseUpdatedEvent extends Event {
    private Integer courseAggregateId;
    private Integer courseVersion;
    private String courseTitle;
    private String courseDescription;
    private Integer courseMaxStudents;

    public EnrollmentCourseUpdatedEvent() {
        super();
    }

    public EnrollmentCourseUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public EnrollmentCourseUpdatedEvent(Integer aggregateId, Integer courseAggregateId, Integer courseVersion, String courseTitle, String courseDescription, Integer courseMaxStudents) {
        super(aggregateId);
        setCourseAggregateId(courseAggregateId);
        setCourseVersion(courseVersion);
        setCourseTitle(courseTitle);
        setCourseDescription(courseDescription);
        setCourseMaxStudents(courseMaxStudents);
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

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public Integer getCourseMaxStudents() {
        return courseMaxStudents;
    }

    public void setCourseMaxStudents(Integer courseMaxStudents) {
        this.courseMaxStudents = courseMaxStudents;
    }

}