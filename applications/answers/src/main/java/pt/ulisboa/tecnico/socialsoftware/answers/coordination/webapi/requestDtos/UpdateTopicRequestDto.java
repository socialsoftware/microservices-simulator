package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicCourseDto;

public class UpdateTopicRequestDto {
    @NotNull
    private String name;
    @NotNull
    private TopicCourseDto course;

    public UpdateTopicRequestDto() {}

    public UpdateTopicRequestDto(String name, TopicCourseDto course) {
        this.name = name;
        this.course = course;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public TopicCourseDto getCourse() {
        return course;
    }

    public void setCourse(TopicCourseDto course) {
        this.course = course;
    }
}
