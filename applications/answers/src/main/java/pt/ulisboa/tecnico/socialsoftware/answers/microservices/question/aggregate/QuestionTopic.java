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
    private AggregateState topicState;
    private Integer topicAggregateId;
    private String topicName;
    private Integer topicVersion;
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
        setTopicState(other.getTopicState());
        setTopicAggregateId(other.getTopicAggregateId());
        setTopicName(other.getTopicName());
        setTopicVersion(other.getTopicVersion());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }


    public QuestionTopicDto buildDto() {
        QuestionTopicDto dto = new QuestionTopicDto();
        dto.setTopicId(getTopicId());
        dto.setTopicState(getTopicState());
        dto.setAggregateId(getTopicAggregateId());
        dto.setName(getTopicName());
        dto.setVersion(getTopicVersion());
        return dto;
    }
}