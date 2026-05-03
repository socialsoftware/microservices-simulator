package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuestionDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;
    private String title;
    private String content;
    private LocalDateTime creationDate;
    private Integer courseAggregateId;
    private Long courseVersion;
    private List<Integer> topicIds = new ArrayList<>();
    private List<Integer> optionKeys = new ArrayList<>();

    public QuestionDto() {}

    public QuestionDto(Question question) {
        setAggregateId(question.getAggregateId());
        setVersion(question.getVersion());
        setState(question.getState());
        setTitle(question.getTitle());
        setContent(question.getContent());
        setCreationDate(question.getCreationDate());
        setAggregateId(question.getAggregateId());
        if (question.getQuestionCourse() != null) {
            setCourseAggregateId(question.getQuestionCourse().getCourseAggregateId());
            setCourseVersion(question.getQuestionCourse().getCourseVersion());
        }
        for (QuestionTopic topic : question.getTopics()) {
            this.topicIds.add(topic.getTopicAggregateId());
        }
        for (Option option : question.getOptions()) {
            this.optionKeys.add(option.getOptionKey());
        }
    }

    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public AggregateState getState() { return state; }
    public void setState(AggregateState state) { this.state = state; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }

    public Integer getCourseAggregateId() { return courseAggregateId; }
    public void setCourseAggregateId(Integer courseAggregateId) { this.courseAggregateId = courseAggregateId; }

    public Long getCourseVersion() { return courseVersion; }
    public void setCourseVersion(Long courseVersion) { this.courseVersion = courseVersion; }

    public List<Integer> getTopicIds() { return topicIds; }
    public void setTopicIds(List<Integer> topicIds) { this.topicIds = topicIds; }

    public List<Integer> getOptionKeys() { return optionKeys; }
    public void setOptionKeys(List<Integer> optionKeys) { this.optionKeys = optionKeys; }
}
