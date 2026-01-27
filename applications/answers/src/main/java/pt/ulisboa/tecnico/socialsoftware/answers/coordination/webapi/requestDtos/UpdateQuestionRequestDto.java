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

public class UpdateQuestionRequestDto {
    @NotNull
    private String title;
    @NotNull
    private String content;
    @NotNull
    private LocalDateTime creationDate;
    @NotNull
    private QuestionCourseDto course;
    @NotNull
    private Set<QuestionTopicDto> topics;
    @NotNull
    private List<OptionDto> options;

    public UpdateQuestionRequestDto() {}

    public UpdateQuestionRequestDto(String title, String content, LocalDateTime creationDate, QuestionCourseDto course, Set<QuestionTopicDto> topics, List<OptionDto> options) {
        this.title = title;
        this.content = content;
        this.creationDate = creationDate;
        this.course = course;
        this.topics = topics;
        this.options = options;
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
    public QuestionCourseDto getCourse() {
        return course;
    }

    public void setCourse(QuestionCourseDto course) {
        this.course = course;
    }
    public Set<QuestionTopicDto> getTopics() {
        return topics;
    }

    public void setTopics(Set<QuestionTopicDto> topics) {
        this.topics = topics;
    }
    public List<OptionDto> getOptions() {
        return options;
    }

    public void setOptions(List<OptionDto> options) {
        this.options = options;
    }
}
