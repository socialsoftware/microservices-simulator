package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;

public class CreateQuestionRequestDto {
    @NotNull
    private CourseDto course;
    @NotNull
    private Set<TopicDto> topics;
    @NotNull
    private String title;
    @NotNull
    private String content;
    @NotNull
    private LocalDateTime creationDate;
    private List<OptionDto> options;

    public CreateQuestionRequestDto() {}

    public CreateQuestionRequestDto(CourseDto course, Set<TopicDto> topics, String title, String content, LocalDateTime creationDate, List<OptionDto> options) {
        this.course = course;
        this.topics = topics;
        this.title = title;
        this.content = content;
        this.creationDate = creationDate;
        this.options = options;
    }

    public CourseDto getCourse() {
        return course;
    }

    public void setCourse(CourseDto course) {
        this.course = course;
    }
    public Set<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(Set<TopicDto> topics) {
        this.topics = topics;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
    public List<OptionDto> getOptions() {
        return options;
    }

    public void setOptions(List<OptionDto> options) {
        this.options = options;
    }
}
