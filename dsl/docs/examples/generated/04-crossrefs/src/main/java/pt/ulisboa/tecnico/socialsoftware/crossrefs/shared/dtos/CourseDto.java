package pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.Course;

public class CourseDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String title;
    private String description;
    private Integer maxStudents;
    private CourseTeacherDto teacher;

    public CourseDto() {
    }

    public CourseDto(Course course) {
        this.aggregateId = course.getAggregateId();
        this.version = course.getVersion();
        this.state = course.getState();
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.maxStudents = course.getMaxStudents();
        this.teacher = course.getTeacher() != null ? new CourseTeacherDto(course.getTeacher()) : null;
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
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

    public CourseTeacherDto getTeacher() {
        return teacher;
    }

    public void setTeacher(CourseTeacherDto teacher) {
        this.teacher = teacher;
    }
}