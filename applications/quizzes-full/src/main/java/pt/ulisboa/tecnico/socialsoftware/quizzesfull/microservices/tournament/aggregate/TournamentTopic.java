package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;

@Entity
public class TournamentTopic {

    @Id
    @GeneratedValue
    private Integer id;

    private Integer topicAggregateId;
    private Long topicVersion;
    private String topicName;

    public TournamentTopic() {}

    public TournamentTopic(TopicDto topicDto) {
        this.topicAggregateId = topicDto.getAggregateId();
        this.topicVersion = topicDto.getVersion();
        this.topicName = topicDto.getName();
    }

    public TournamentTopic(TournamentTopic other) {
        this.topicAggregateId = other.getTopicAggregateId();
        this.topicVersion = other.getTopicVersion();
        this.topicName = other.getTopicName();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getTopicAggregateId() { return topicAggregateId; }
    public void setTopicAggregateId(Integer topicAggregateId) { this.topicAggregateId = topicAggregateId; }

    public Long getTopicVersion() { return topicVersion; }
    public void setTopicVersion(Long topicVersion) { this.topicVersion = topicVersion; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
}
