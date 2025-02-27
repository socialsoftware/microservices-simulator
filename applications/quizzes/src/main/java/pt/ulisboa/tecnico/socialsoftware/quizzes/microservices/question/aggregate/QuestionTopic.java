package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;

@Entity
public class QuestionTopic {
    @Id
    @GeneratedValue
    private Long id;
    private Integer topicAggregateId;
    private String topicName;
    private Integer topicVersion;
    private Aggregate.AggregateState state;
    @ManyToOne
    private Question question;

    public QuestionTopic() {
    }

    public QuestionTopic (TopicDto topicDto) {
        setTopicAggregateId(topicDto.getAggregateId());
        setTopicName(topicDto.getName());
        setTopicVersion(topicDto.getVersion());
    }

    public QuestionTopic(QuestionTopic other) {
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

    public Aggregate.AggregateState getState() {
        return state;
    }

    public void setState(Aggregate.AggregateState state) {
        this.state = state;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public TopicDto buildDto() {
        TopicDto dto = new TopicDto();
        dto.setAggregateId(this.topicAggregateId);
        dto.setName(this.topicName);
        dto.setVersion(this.topicVersion);
        return dto;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getTopicAggregateId();
        hash = 31 * hash + (getTopicVersion() == null ? 0 : getTopicVersion().hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof QuestionTopic)) {
            return false;
        }
        QuestionTopic tournamentTopic = (QuestionTopic) obj;

        return getTopicAggregateId() != null && getTopicAggregateId().equals(tournamentTopic.getTopicAggregateId()) &&
                getTopicVersion() != null && getTopicVersion().equals(tournamentTopic.getTopicVersion());
    }
}
