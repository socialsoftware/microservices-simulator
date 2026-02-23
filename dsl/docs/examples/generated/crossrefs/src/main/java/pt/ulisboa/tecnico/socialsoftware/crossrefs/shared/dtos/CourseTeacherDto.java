package pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.CourseTeacher;

public class CourseTeacherDto implements Serializable {
    private String name;
    private String email;
    private String department;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public CourseTeacherDto() {
    }

    public CourseTeacherDto(CourseTeacher courseTeacher) {
        this.name = courseTeacher.getTeacherName();
        this.email = courseTeacher.getTeacherEmail();
        this.department = courseTeacher.getTeacherDepartment();
        this.aggregateId = courseTeacher.getTeacherAggregateId();
        this.version = courseTeacher.getTeacherVersion();
        this.state = courseTeacher.getTeacherState();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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
}