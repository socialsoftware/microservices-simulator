package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

public class QuestionTopicDto {
    private Integer topicAggregateId;
    private String topicName;
    private Long topicVersion;
    private Integer courseAggregateId;

    public QuestionTopicDto() {
    }

    public QuestionTopicDto(Integer topicAggregateId, String topicName, Long topicVersion, Integer courseAggregateId) {
        this.topicAggregateId = topicAggregateId;
        this.topicName = topicName;
        this.topicVersion = topicVersion;
        this.courseAggregateId = courseAggregateId;
    }

    public QuestionTopicDto(QuestionTopic questionTopic) {
        this.topicAggregateId = questionTopic.getTopicAggregateId();
        this.topicName = questionTopic.getTopicName();
        this.topicVersion = questionTopic.getTopicVersion();
        this.courseAggregateId = questionTopic.getCourseAggregateId();
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
