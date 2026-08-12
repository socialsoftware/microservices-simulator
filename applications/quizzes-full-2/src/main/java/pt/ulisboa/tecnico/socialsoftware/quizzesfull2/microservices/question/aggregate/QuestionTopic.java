package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_topics")
public class QuestionTopic {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer topicAggregateId;
    private String topicName;
    private Long topicVersion;
    private Integer courseAggregateId;

    public QuestionTopic() {
    }

    public QuestionTopic(Integer topicAggregateId, String topicName, Long topicVersion, Integer courseAggregateId) {
        this.topicAggregateId = topicAggregateId;
        this.topicName = topicName;
        this.topicVersion = topicVersion;
        this.courseAggregateId = courseAggregateId;
    }

    public QuestionTopic(QuestionTopic other) {
        this.topicAggregateId = other.getTopicAggregateId();
        this.topicName = other.getTopicName();
        this.topicVersion = other.getTopicVersion();
        this.courseAggregateId = other.getCourseAggregateId();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public void setTopicAggregateId(Integer topicAggregateId) {
        this.topicAggregateId = topicAggregateId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Long getTopicVersion() {
        return topicVersion;
    }

    public void setTopicVersion(Long topicVersion) {
        this.topicVersion = topicVersion;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }
}
