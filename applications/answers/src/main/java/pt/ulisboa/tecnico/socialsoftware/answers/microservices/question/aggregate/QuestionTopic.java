package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

@Entity
public class QuestionTopic {
    @Id
    @GeneratedValue
    private Long id;
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
        setTopicAggregateId(topicDto.getAggregateId());
        setTopicVersion(topicDto.getVersion());
        setTopicName(topicDto.getName());
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


    public QuestionTopicDto buildDto() {
        QuestionTopicDto dto = new QuestionTopicDto();
        dto.setTopicId(getTopicId());
        dto.setAggregateId(getTopicAggregateId());
        dto.setName(getTopicName());
        dto.setVersion(getTopicVersion());
        dto.setTopicState(getTopicState());
        return dto;
    }
}