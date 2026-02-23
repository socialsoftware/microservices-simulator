package pt.ulisboa.tecnico.socialsoftware.crossrefs.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;
import java.util.Set;
import java.time.LocalDateTime;

public class CreateEnrollmentRequestDto {
    @NotNull
    private CourseDto course;
    @NotNull
    private Set<TeacherDto> teachers;
    @NotNull
    private LocalDateTime enrollmentDate;
    @NotNull
    private Boolean active;

    public CreateEnrollmentRequestDto() {}

    public CreateEnrollmentRequestDto(CourseDto course, Set<TeacherDto> teachers, LocalDateTime enrollmentDate, Boolean active) {
        this.course = course;
        this.teachers = teachers;
        this.enrollmentDate = enrollmentDate;
        this.active = active;
    }

    public CourseDto getCourse() {
        return course;
    }

    public void setCourse(CourseDto course) {
        this.course = course;
    }
    public Set<TeacherDto> getTeachers() {
        return teachers;
    }

    public void setTeachers(Set<TeacherDto> teachers) {
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
