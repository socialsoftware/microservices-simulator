package pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentCourse;

public class EnrollmentCourseDto implements Serializable {
    private String title;
    private String description;
    private Integer maxStudents;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public EnrollmentCourseDto() {
    }

    public EnrollmentCourseDto(EnrollmentCourse enrollmentCourse) {
        this.title = enrollmentCourse.getCourseTitle();
        this.description = enrollmentCourse.getCourseDescription();
        this.maxStudents = enrollmentCourse.getCourseMaxStudents();
        this.aggregateId = enrollmentCourse.getCourseAggregateId();
        this.version = enrollmentCourse.getCourseVersion();
        this.state = enrollmentCourse.getCourseState() != null ? enrollmentCourse.getCourseState().name() : null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}