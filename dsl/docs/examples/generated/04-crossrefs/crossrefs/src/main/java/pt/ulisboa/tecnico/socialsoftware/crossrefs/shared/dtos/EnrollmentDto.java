package pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentTeacher;

public class EnrollmentDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private EnrollmentCourseDto course;
    private Set<EnrollmentTeacherDto> teachers;
    private LocalDateTime enrollmentDate;
    private Boolean active;

    public EnrollmentDto() {
    }

    public EnrollmentDto(Enrollment enrollment) {
        this.aggregateId = enrollment.getAggregateId();
        this.version = enrollment.getVersion();
        this.state = enrollment.getState();
        this.course = enrollment.getCourse() != null ? new EnrollmentCourseDto(enrollment.getCourse()) : null;
        this.teachers = enrollment.getTeachers() != null ? enrollment.getTeachers().stream().map(EnrollmentTeacher::buildDto).collect(Collectors.toSet()) : null;
        this.enrollmentDate = enrollment.getEnrollmentDate();
        this.active = enrollment.getActive();
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

    public EnrollmentCourseDto getCourse() {
        return course;
    }

    public void setCourse(EnrollmentCourseDto course) {
        this.course = course;
    }

    public Set<EnrollmentTeacherDto> getTeachers() {
        return teachers;
    }

    public void setTeachers(Set<EnrollmentTeacherDto> teachers) {
        this.teachers = teachers;
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}