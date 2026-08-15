package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionDto {
    private Integer aggregateId;
    private Long version;
    private String title;
    private String content;
    private LocalDateTime creationDate;
    private Integer courseAggregateId;
    private List<OptionDto> options = new ArrayList<>();
    private List<QuestionTopicDto> topics = new ArrayList<>();

    public QuestionDto() {
    }

    public QuestionDto(Integer aggregateId, Long version, String title, String content, LocalDateTime creationDate,
                       Integer courseAggregateId, List<OptionDto> options, List<QuestionTopicDto> topics) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.title = title;
        this.content = content;
        this.creationDate = creationDate;
        this.courseAggregateId = courseAggregateId;
        this.options = options;
        this.topics = topics;
    }

    public QuestionDto(Question question) {
        this.aggregateId = question.getAggregateId();
        this.version = question.getVersion();
        this.title = question.getTitle();
        this.content = question.getContent();
        this.creationDate = question.getCreationDate();
        this.courseAggregateId = question.getCourseAggregateId();
        this.options = question.getOptions().stream()
                .map(OptionDto::new)
                .collect(Collectors.toList());
        this.topics = question.getTopics().stream()
                .map(QuestionTopicDto::new)
                .collect(Collectors.toList());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public List<OptionDto> getOptions() {
        return options;
    }

    public void setOptions(List<OptionDto> options) {
        this.options = options;
    }

    public List<QuestionTopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<QuestionTopicDto> topics) {
        this.topics = topics;
    }
}
