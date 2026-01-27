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
    private Integer topicAggregateId;
    private Integer topicVersion;
    private Integer topicId;
    private AggregateState topicState;
    private String topicName;
    @OneToOne
    private Question question;

    public QuestionTopic() {

    }

    public QuestionTopic(TopicDto topicDto) {
        setTopicAggregateId(topicDto.getAggregateId());
        setTopicVersion(topicDto.getVersion());
        setTopicState(topicDto.getState());
        setTopicName(topicDto.getName());
    }

    public QuestionTopic(QuestionTopic other) {
        setTopicVersion(other.getTopicVersion());
        setTopicId(other.getTopicId());
        setTopicState(other.getTopicState());
        setTopicName(other.getTopicName());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTopicAggregateId() {
        return topicAggregateId;
    }

    public void setTopicAggregateId(Integer topicAggregateId) {
        this.topicAggregateId = topicAggregateId;
    }

    public Integer getTopicVersion() {
        return topicVersion;
    }

    public void setTopicVersion(Integer topicVersion) {
        this.topicVersion = topicVersion;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public AggregateState getTopicState() {
        return topicState;
    }

    public void setTopicState(AggregateState topicState) {
        this.topicState = topicState;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }


    public QuestionTopicDto buildDto() {
        QuestionTopicDto dto = new QuestionTopicDto();
        dto.setAggregateId(getTopicAggregateId());
        dto.setVersion(getTopicVersion());
        dto.setTopicId(getTopicId());
        dto.setState(getTopicState());
        dto.setName(getTopicName());
        return dto;
    }
}