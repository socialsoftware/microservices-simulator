package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;

public class QuestionTopicDto implements Serializable {
    private Integer topicId;
    private AggregateState state;
    private String name;
    private Integer aggregateId;
    private Integer version;

    public QuestionTopicDto() {
    }

    public QuestionTopicDto(QuestionTopic questionTopic) {
        this.topicId = questionTopic.getTopicId();
        this.state = questionTopic.getTopicState();
        this.name = questionTopic.getTopicName();
        this.aggregateId = questionTopic.getTopicAggregateId();
        this.version = questionTopic.getTopicVersion();
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}