package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;

public class QuestionDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String title;
    private String content;
    private LocalDateTime creationDate;
    private QuestionCourseDto course;
    private Set<QuestionTopicDto> topics;
    private List<OptionDto> options;

    public QuestionDto() {
    }

    public QuestionDto(Question question) {
        this.aggregateId = question.getAggregateId();
        this.version = question.getVersion();
        this.state = question.getState();
        this.title = question.getTitle();
        this.content = question.getContent();
        this.creationDate = question.getCreationDate();
        this.course = question.getCourse() != null ? new QuestionCourseDto(question.getCourse()) : null;
        this.topics = question.getTopics() != null ? question.getTopics().stream().map(QuestionTopic::buildDto).collect(Collectors.toSet()) : null;
        this.options = question.getOptions() != null ? question.getOptions().stream().map(OptionDto::new).collect(Collectors.toList()) : null;
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