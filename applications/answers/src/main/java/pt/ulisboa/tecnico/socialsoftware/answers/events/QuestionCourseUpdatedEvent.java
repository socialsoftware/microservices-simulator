package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class QuestionCourseUpdatedEvent extends Event {
    @Column(name = "question_course_updated_event_course_aggregate_id")
    private Integer courseAggregateId;
    @Column(name = "question_course_updated_event_course_version")
    private Integer courseVersion;
    @Column(name = "question_course_updated_event_course_name")
    private String courseName;

    public QuestionCourseUpdatedEvent() {
        super();
    }

    public QuestionCourseUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuestionCourseUpdatedEvent(Integer aggregateId, Integer courseAggregateId, Integer courseVersion, String courseName) {
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