package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;

public class CreateTopicRequestDto {
    @NotNull
    private CourseDto course;
    @NotNull
    private String name;

    public CreateTopicRequestDto() {}

    public CreateTopicRequestDto(CourseDto course, String name) {
        this.course = course;
        this.name = name;
    }

    public CourseDto getCourse() {
        return course;
    }

    public void setCourse(CourseDto course) {
        this.course = course;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
