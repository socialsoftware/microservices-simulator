package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;

public class QuestionTopicDto implements Serializable {
    private Integer topicId;
    private Integer aggregateId;
    private String name;
    private Integer version;
    private AggregateState topicState;

    public QuestionTopicDto() {
    }

    public QuestionTopicDto(QuestionTopic questionTopic) {
        this.topicId = questionTopic.getTopicId();
        this.aggregateId = questionTopic.getTopicAggregateId();
        this.name = questionTopic.getTopicName();
        this.version = questionTopic.getTopicVersion();
        this.topicState = questionTopic.getTopicState();
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getTopicState() {
        return topicState;
    }

    public void setTopicState(AggregateState topicState) {
        this.topicState = topicState;
    }
}