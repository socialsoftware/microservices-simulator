package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicDto;

@Entity
public class QuestionTopic {
    @Id
    @GeneratedValue
    private Integer topicId;
    private Integer topicAggregateId;
    private String topicName;
    private Integer topicVersion;
    private AggregateState topicState;
    @OneToOne
    private Question question;

    public QuestionTopic() {

    }

    public QuestionTopic(TopicDto topicDto) {
        setName(topicDto.getName());
    }

    public QuestionTopic(QuestionTopic other) {
        setTopicAggregateId(other.getTopicAggregateId());
        setTopicName(other.getTopicName());
        setTopicVersion(other.getTopicVersion());
        setTopicState(other.getTopicState());
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
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

    public Integer getTopicVersion() {
        return topicVersion;
    }

    public void setTopicVersion(Integer topicVersion) {
        this.topicVersion = topicVersion;
    }

    public AggregateState getTopicState() {
        return topicState;
    }

    public void setTopicState(AggregateState topicState) {
        this.topicState = topicState;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }


    public TopicDto buildDto() {
        TopicDto dto = new TopicDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        return dto;
    }
}