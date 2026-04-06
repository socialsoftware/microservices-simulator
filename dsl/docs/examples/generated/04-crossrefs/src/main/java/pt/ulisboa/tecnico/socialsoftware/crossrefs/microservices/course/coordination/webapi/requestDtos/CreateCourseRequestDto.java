package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;

public class CreateCourseRequestDto {
    @NotNull
    private TeacherDto teacher;
    @NotNull
    private String title;
    @NotNull
    private String description;
    @NotNull
    private Integer maxStudents;

    public CreateCourseRequestDto() {}

    public CreateCourseRequestDto(TeacherDto teacher, String title, String description, Integer maxStudents) {
        this.teacher = teacher;
        this.title = title;
        this.description = description;
        this.maxStudents = maxStudents;
    }

    public TeacherDto getTeacher() {
        return teacher;
    }

    public void setTeacher(TeacherDto teacher) {
        this.teacher = teacher;
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
}
